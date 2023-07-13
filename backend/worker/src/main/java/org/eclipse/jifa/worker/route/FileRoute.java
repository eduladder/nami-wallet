/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.jifa.worker.route;

import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.util.Strings;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.request.PagingRequest;
import org.eclipse.jifa.common.util.HTTPRespGuarder;
import org.eclipse.jifa.common.util.PageViewBuilder;
import org.eclipse.jifa.common.vo.FileInfo;
import org.eclipse.jifa.common.vo.PageView;
import org.eclipse.jifa.common.vo.TransferProgress;
import org.eclipse.jifa.common.vo.TransferringFile;
import org.eclipse.jifa.worker.Global;
import org.eclipse.jifa.worker.support.FileSupport;
import org.eclipse.jifa.worker.support.heapdump.TransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.eclipse.jifa.common.Constant.EMPTY_STRING;
import static org.eclipse.jifa.common.util.Assertion.ASSERT;

class FileRoute extends BaseRoute {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRoute.class);

    @RouteMeta(path = "/files")
    void list(Future<PageView<FileInfo>> future, @ParamKey("type") FileType type, PagingRequest paging) {
        List<FileInfo> info = FileSupport.info(type);
        info.sort((i1, i2) -> Long.compare(i2.getCreationTime(), i1.getCreationTime()));
        future.complete(PageViewBuilder.build(info, paging));
    }

    @RouteMeta(path = "/file")
    void file(Future<FileInfo> future, @ParamKey("type") FileType type, @ParamKey("name") String name) {
        future.complete(FileSupport.info(type, name));
    }

    @RouteMeta(path = "/file/delete", method = HttpMethod.POST)
    void delete(Future<Void> future, @ParamKey("type") FileType type, @ParamKey("name") String name) {
        FileSupport.delete(type, name);
        future.complete();
    }

    @RouteMeta(path = "/publicKey")
    void publicKeys(Future<String> future) {
        if (FileSupport.PUB_KEYS.size() > 0) {
            future.complete(FileSupport.PUB_KEYS.get(0));
        } else {
            future.complete(EMPTY_STRING);
        }
    }

    private String decorateFileName(String fileName) {
        return System.currentTimeMillis() + "-" + fileName;
    }

    private String extractFileName(String path) {
        return path.substring(path.lastIndexOf(File.separatorChar) + 1);
    }

    @RouteMeta(path = "/file/transferByURL", method = HttpMethod.POST)
    void transferByURL(Future<TransferringFile> future, @ParamKey("type") FileType fileType,
                       @ParamKey("url") String url, @ParamKey(value = "fileName", mandatory = false) String fileName) {

        String originalName = extractFileName(url);

        fileName = Strings.isNotBlank(fileName) ? fileName : decorateFileName(originalName);

        TransferListener listener = FileSupport.createTransferListener(fileType, originalName, fileName);

        FileSupport.transferByURL(url, fileType, fileName, listener, future);
    }

    @RouteMeta(path = "/file/transferByOSS", method = HttpMethod.POST)
    void transferByOSS(Future<TransferringFile> future, @ParamKey("type") FileType fileType,
                       @ParamKey("endpoint") String endpoint, @ParamKey("accessKeyId") String accessKeyId,
                       @ParamKey("accessKeySecret") String accessKeySecret, @ParamKey("bucketName") String bucketName,
                       @ParamKey("objectName") String objectName,
                       @ParamKey(value = "fileName", mandatory = false) String fileName) {

        String originalName = extractFileName(objectName);
        fileName = Strings.isNotBlank(fileName) ? fileName : decorateFileName(originalName);

        TransferListener listener = FileSupport.createTransferListener(fileType, originalName, fileName);

        FileSupport.transferByOSS(endpoint, accessKeyId, accessKeySecret, bucketName, objectName,
                                  fileType, fileName, listener, future);
    }

    @RouteMeta(path = "/file/transferBySCP", method = HttpMethod.POST)
    void transferBySCP(Future<TransferringFile> future, @ParamKey("type") FileType fileType,
                       @ParamKey("hostname") String hostname, @ParamKey("path") String path,
                       @ParamKey("user") String user, @ParamKey("usePublicKey") boolean usePublicKey,
                       @ParamKey(value = "password", mandatory = false) String password,
                       @ParamKey(value = "fileName", mandatory = false) String fileName) {

        if (!usePublicKey) {
            ASSERT.isTrue(password != null && password.length() > 0,
                          "Must provide password if you don't use public key");
        }

        String originalName = extractFileName(path);
        fileName = Strings.isNotBlank(fileName) ? fileName : decorateFileName(extractFileName(path));
        TransferListener listener = FileSupport.createTransferListener(fileType, originalName, fileName);
        // do transfer
        if (usePublicKey) {
            FileSupport.transferBySCP(user, hostname, path, fileType, fileName, listener, future);
        } else {
            FileSupport.transferBySCP(user, password, hostname, path, fileType, fileName, listener, future);
        }
    }

    @RouteMeta(path = "/file/transferByFileSystem", method = HttpMethod.POST)
    void transferByFileSystem(Future<TransferringFile> future, @ParamKey("type") FileType fileType,
                              @ParamKey("path") String path, @ParamKey("move") boolean move) {
        File src = new File(path);
        ASSERT.isTrue(src.exists() && !src.isDirectory(), "Illegal path");

        String originalName = extractFileName(path);
        String fileName = decorateFileName(originalName);

        future.complete(new TransferringFile(fileName));
        TransferListener listener = FileSupport.createTransferListener(fileType, originalName, fileName);

        listener.setTotalSize(src.length());
        listener.updateState(ProgressState.IN_PROGRESS);
        if (move) {
            Global.VERTX.fileSystem().moveBlocking(path, FileSupport.filePath(fileType, fileName));
        } else {
            Global.VERTX.fileSystem().copyBlocking(path, FileSupport.filePath(fileType, fileName));
        }
        listener.setTransferredSize(listener.getTotalSize());
        listener.updateState(ProgressState.SUCCESS);
    }

    @RouteMeta(path = "/file/transferProgress")
    void transferProgress(Future<TransferProgress> future, @ParamKey("type") FileType type,
                          @ParamKey("name") String name) {
        TransferListener listener = FileSupport.getTransferListener(name);
        if (listener != null) {
            TransferProgress progress = new TransferProgress();
            progress.setTotalSize(listener.getTotalSize());
            progress.setTransferredSize(listener.getTransferredSize());
            progress.setMessage(listener.getErrorMsg());
            if (listener.getTotalSize() > 0) {
                progress.setPercent((double) listener.getTransferredSize() / (double) listener.getTotalSize());
            }
            progress.setState(listener.getState());

            if (progress.getState() == ProgressState.SUCCESS || progress.getState() == ProgressState.ERROR) {
                FileSupport.removeTransferListener(name);
            }
            future.complete(progress);
        } else {
            FileInfo info = FileSupport.info(type, name);
            ASSERT.notNull(info);
            if (info.getTransferState() == ProgressState.IN_PROGRESS
                || info.getTransferState() == ProgressState.NOT_STARTED) {
                LOGGER.warn("Illegal file {} state", name);
                info.setTransferState(ProgressState.ERROR);
                FileSupport.save(info);
            }
            TransferProgress progress = new TransferProgress();
            progress.setState(info.getTransferState());
            if (progress.getState() == ProgressState.SUCCESS) {
                progress.setPercent(1.0);
                progress.setTotalSize(info.getSize());
                progress.setTransferredSize(info.getSize());
            }
            future.complete(progress);
        }
    }

    @RouteMeta(path = "/file/upload", method = HttpMethod.POST)
    void upload(RoutingContext context, @ParamKey("type") FileType type) {
        Set<FileUpload> fileUploads = context.fileUploads();
        for (FileUpload upload : fileUploads) {
            String fileName = decorateFileName(upload.fileName());
            FileSupport.initInfoFile(type, upload.fileName(), fileName);
            FileSystem fileSystem = context.vertx().fileSystem();
            fileSystem.moveBlocking(upload.uploadedFileName(), FileSupport.filePath(type, fileName));
            FileSupport.updateTransferState(type, fileName, ProgressState.SUCCESS);
        }
        HTTPRespGuarder.ok(context);
    }

    @RouteMeta(path = "/file/download/:fileType/:filename", contentType = {} /* keep content-type empty */)
    void download(RoutingContext context, @ParamKey("fileType") FileType fileType, @ParamKey("filename") String name) {
        File file = new File(FileSupport.filePath(fileType, name));
        ASSERT.isTrue(file.exists(), "File doesn't exist!");
        HttpServerResponse response = context.response();
        response.sendFile(file.getAbsolutePath(), event -> {
            if (!response.ended()) {
                response.end();
            }
        });
    }

    @RouteMeta(path = "/file/getOrGenInfo", method = HttpMethod.POST)
    void getOrGenInfo(Future<FileInfo> future, @ParamKey("fileType") FileType fileType,
                      @ParamKey("filename") String name) {
        future.complete(FileSupport.getOrGenInfo(fileType, name));
    }
}
