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
package org.eclipse.jifa.worker;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.eclipse.jifa.worker.support.FileSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.eclipse.jifa.common.util.Assertion.ASSERT;

public class Global {

    private static final Logger LOGGER = LoggerFactory.getLogger(Starter.class);

    public static Vertx VERTX;

    public static String HOST;

    public static int PORT;

    private static JsonObject CONFIG;

    private static String WORKSPACE;

    private static boolean initialized;

    static synchronized void init(Vertx vertx, String host, int port, JsonObject config) {
        if (initialized) {
            return;
        }

        VERTX = vertx;
        HOST = host;
        PORT = port;
        CONFIG = config;

        WORKSPACE = CONFIG.getString(Constant.ConfigKey.WORKSPACE, Constant.Misc.DEFAULT_WORKSPACE);
        LOGGER.debug("Workspace: {}", WORKSPACE);

        File workspaceDir = new File(WORKSPACE);
        if (workspaceDir.exists()) {
            ASSERT.isTrue(workspaceDir.isDirectory(), "Workspace must be directory");
        } else {
            ASSERT.isTrue(workspaceDir.mkdirs(),
                          () -> "Can not create workspace: " + workspaceDir.getAbsolutePath());
        }

        FileSupport.init();

        initialized = true;
    }

    public static String stringConfig(String key) {
        return CONFIG.getString(key);
    }

    public static String workspace() {
        return WORKSPACE;
    }
}
