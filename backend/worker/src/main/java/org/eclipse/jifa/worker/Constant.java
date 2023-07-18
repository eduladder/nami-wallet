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

public interface Constant extends org.eclipse.jifa.common.Constant {

    interface Misc {
        String VERTX_CONFIG_KEY = "jifa.vertx.config";
        String WORKER_CONFIG_KEY = "jifa.worker.config";
        String DEFAULT_VERTX_CONFIG = "vertx-config.json";
        String DEFAULT_WORKER_CONFIG = "worker-config.json";

        String DEFAULT_WORKSPACE = System.getProperty("user.home") + java.io.File.separator + "jifa_workspace";

        String DEFAULT_HOST = "0.0.0.0";

        String WEB_ROOT_KEY = "jifa.webroot";
    }

    interface Heap {
        String TOTAL_SIZE_KEY = "totalSize";
        String SHALLOW_HEAP_KEY = "shallowHeap";
        String RETAINED_HEAP_KEY = "retainedHeap";
    }

    interface DomTree {
        String SHALLOW_SIZE_KEY = "shallowSize";
        String RETAINED_SIZE_KEY = "retainedSize";
        String LABEL_KEY = "label";
        String SUFFIX_KEY = "suffix";
        String OBJECT_ID_KEY = "objectId";
        String OBJECT_TYPE_KEY = "objectType";
        String GC_ROOT_KEY = "gCRoot";
        String PERCENT_KEY = "percent";
    }

    interface Histogram {
        String NUM_OBJECTS_KEY = "numberOfObjects";
        String SHALLOW_SIZE_KEY = "shallowSize";
        String RETAINED_SIZE_KEY = "retainedSize";
        String LABEL_KEY = "label";
        String OBJECT_ID_KEY = "objectId";
        String TYPE_KEY = "type";        
    }

    interface Threads {
        String OBJECT_ID_KEY = "objectId";
        String OBJECT_KEY = "object";
        String NAME_KEY = "name";
        String SHALLOW_SIZE_KEY = "shallowSize";
        String RETAINED_SIZE_KEY = "retainedSize";
        String CONTEXT_CLASS_LOADER_KEY = "contextClassLoader";
        String HAS_STACK_KEY = "hasStack";
        String DAEMON_KEY = "daemon";
    }

    interface StackFrame {
        String STACK_NAME_KEY = "stack";
        String HAS_LOCAL_KEY = "hasLocal";
        String STACK_ID_KEY = "stackId";
    }

    interface Outbounds {
        String OBJECT_ID_KEY = "objectId";
        String PREFIX_KEY = "prefix";
        String LABEL_KEY = "label";
        String SUFFIX_KEY = "suffix";
        String SHALLOW_SIZE_KEY = "shallowSize";
        String RETAINED_SIZE_KEY = "retainedSize";
        String HAS_INBOUND_KEY = "hasInbound";
        String HAS_OUTBOUND_KEY = "hasOutbound";
        String OBJECT_TYPE_KEY = "objectType";
        String GC_ROOT_KEY = "gCRoot";        
    }

    interface API {
        String HEAP_DUMP_API_PREFIX = "http://localhost:8102/jifa-api/heap-dump";
    }

    interface File {
        String INFO_FILE_SUFFIX = "-info.json";
    }

    interface ConfigKey {
        String WORKSPACE = "workspace";
        String API_PREFIX = "api.prefix";
        String SERVER_HOST_KEY = "server.host";
        String SERVER_PORT_KEY = "server.port";
    }
}
