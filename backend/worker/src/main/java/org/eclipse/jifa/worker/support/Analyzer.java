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
package org.eclipse.jifa.worker.support;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.vertx.core.Future;
import org.eclipse.jifa.common.aux.JifaException;
import org.eclipse.jifa.common.enums.FileType;
import org.eclipse.jifa.common.enums.ProgressState;
import org.eclipse.jifa.common.util.ErrorUtil;
import org.eclipse.jifa.common.util.FileUtil;
import org.eclipse.jifa.worker.support.heapdump.SnapshotContext;
import org.eclipse.mat.snapshot.SnapshotFactory;
import org.eclipse.mat.util.IProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.jifa.common.enums.FileType.HEAP_DUMP;
import static org.eclipse.jifa.common.util.Assertion.ASSERT;
import static org.eclipse.jifa.worker.support.heapdump.HeapDumpSupport.VOID_LISTENER;

public class Analyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Analyzer.class);

    private Map<String, AnalysisProgressListener> listeners;
    private Cache<String, Object> cache;

    private Analyzer() {
        listeners = new HashMap<>();
        cache = CacheBuilder.newBuilder().build();
    }

    private static <T> T getOrBuild(String key, Builder<T> builder) {
        T result = getInstance().getCacheValueIfPresent(key);
        if (result != null) {
            return result;
        }

        synchronized (key.intern()) {
            result = getInstance().getCacheValueIfPresent(key);
            if (result != null) {
                return result;
            }
            try {
                result = builder.build(key);
            } catch (Throwable t) {
                throw new JifaException(t);
            }
            getInstance().putCacheValue(key, result);
            return result;
        }
    }

    private static SnapshotContext getOrOpenSnapshotContext(String heapFile,
                                                            Map<String, String> option, IProgressListener listener) {
        return getOrBuild(heapFile, key -> new SnapshotContext(
            SnapshotFactory.openSnapshot(new File(FileSupport.filePath(HEAP_DUMP, heapFile)), option,
                                         listener)));
    }

    public static SnapshotContext getOrOpenSnapshotContext(String heapFile) {
        return getOrOpenSnapshotContext(heapFile, Collections.emptyMap(), VOID_LISTENER);
    }

    public static Analyzer getInstance() {
        return Singleton.INSTANCE;
    }

    public boolean isFirstAnalysis(FileType fileType, String file) {
        switch (fileType) {
            case HEAP_DUMP:
                return !new File(FileSupport.indexPath(fileType, file)).exists() &&
                       !new File(FileSupport.errorLogPath(fileType, file)).exists() &&
                       getFileListener(file) == null;
            default:
                throw new IllegalArgumentException(fileType.name());
        }
    }

    public void analyze(Future<Void> future, FileType fileType, String fileName, Map<String, String> options) {
        AnalysisProgressListener progressListener;

        if (getCacheValueIfPresent(fileName) != null ||
            new File(FileSupport.errorLogPath(fileType, fileName)).exists()) {
            future.complete();
            return;
        }

        progressListener = new AnalysisProgressListener();
        boolean success = putFileListener(fileName, progressListener);
        future.complete();

        if (success) {
            try {
                switch (fileType) {
                    case HEAP_DUMP:
                        getOrOpenSnapshotContext(fileName, options, progressListener);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                File log = new File(FileSupport.errorLogPath(fileType, fileName));
                FileUtil.write(log, progressListener.log(), false);
                FileUtil.write(log, ErrorUtil.toString(e), true);
            } finally {
                removeFileListener(fileName);
            }
        }
    }

    public void clean(FileType fileType, String fileName) {
        clearCacheValue(fileName);

        File errorLog = new File(FileSupport.errorLogPath(fileType, fileName));
        if (errorLog.exists()) {
            ASSERT.isTrue(errorLog.delete(), "Delete error log failed");
        }

        if (getFileListener(fileName) != null) {
            return;
        }

        File index = new File(FileSupport.indexPath(fileType, fileName));
        if (index.exists()) {
            ASSERT.isTrue(index.delete(), "Delete index file failed");
        }
    }

    public void release(String fileName) {
        clearCacheValue(fileName);
    }

    public org.eclipse.jifa.common.vo.Progress pollProgress(FileType fileType, String fileName) {
        AnalysisProgressListener progressListener = getFileListener(fileName);

        if (progressListener == null) {
            org.eclipse.jifa.common.vo.Progress progress = buildProgressIfFinished(fileType, fileName);
            ASSERT.notNull(progress);
            return progress;
        } else {
            org.eclipse.jifa.common.vo.Progress progress = new org.eclipse.jifa.common.vo.Progress();
            progress.setState(ProgressState.IN_PROGRESS);
            progress.setMessage(progressListener.log());
            progress.setPercent(progressListener.percent());
            return progress;
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> T getCacheValueIfPresent(String key) {
        return (T) cache.getIfPresent(key);
    }

    private synchronized void putCacheValue(String key, Object value) {
        cache.put(key, value);
        LOGGER.info("Put cache: {}", key);
    }

    private synchronized void clearCacheValue(String key) {
        Object value = cache.getIfPresent(key);
        if (value instanceof SnapshotContext) {
            SnapshotFactory.dispose(((SnapshotContext) value).getSnapshot());
        }
        cache.invalidate(key);
        LOGGER.info("Clear cache: {}", key);
    }

    private synchronized AnalysisProgressListener getFileListener(String fileName) {
        return listeners.get(fileName);
    }

    private synchronized boolean putFileListener(String fileName, AnalysisProgressListener listener) {
        if (listeners.containsKey(fileName)) {
            return false;
        }
        listeners.put(fileName, listener);
        return true;
    }

    private synchronized void removeFileListener(String fileName) {
        listeners.remove(fileName);
    }

    private org.eclipse.jifa.common.vo.Progress buildProgressIfFinished(FileType fileType, String fileName) {
        if (getCacheValueIfPresent(fileName) != null) {
            org.eclipse.jifa.common.vo.Progress result = new org.eclipse.jifa.common.vo.Progress();
            result.setPercent(1);
            result.setState(ProgressState.SUCCESS);
            return result;
        }

        File failed = new File(FileSupport.errorLogPath(fileType, fileName));
        if (failed.exists()) {
            org.eclipse.jifa.common.vo.Progress result = new org.eclipse.jifa.common.vo.Progress();
            result.setState(ProgressState.ERROR);
            result.setMessage(FileUtil.content(failed));
            return result;
        }
        return null;
    }

    interface Builder<T> {
        T build(String key) throws Throwable;
    }

    private static class Singleton {
        static Analyzer INSTANCE = new Analyzer();
    }
}
