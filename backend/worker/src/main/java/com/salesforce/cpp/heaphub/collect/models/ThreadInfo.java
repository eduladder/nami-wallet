package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

public class ThreadInfo {
        private String heapId;
        private long createdAt;
        private int objectId; // done
        private String object; // done object_label
        private String name; // done
        private long shallowSize; // done
        private long retainedSize; // done
        private String contextClassLoader; // done
        private boolean hasStack; // done
        private boolean daemon; // done

        public ThreadInfo(JsonObject obj, String heapName, long analysisTime) {
            this.objectId = obj.getInteger(Constant.Threads.OBJECT_ID_KEY);
            this.object = obj.getString(Constant.Threads.OBJECT_KEY);
            this.name = obj.getString(Constant.Threads.NAME_KEY);
            this.shallowSize = obj.getLong(Constant.Threads.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Threads.RETAINED_SIZE_KEY);
            this.contextClassLoader = obj.getString(Constant.Threads.CONTEXT_CLASS_LOADER_KEY);
            this.hasStack = obj.getBoolean(Constant.Threads.HAS_STACK_KEY);
            this.daemon = obj.getBoolean(Constant.Threads.DAEMON_KEY);
            heapId = heapName;
            createdAt = analysisTime;
        }

        public int getObjectId() {
            return objectId;
        }

        public String getObject() {
            return object;
        }
        public String getName() {
            return name;
        }
        public long getShallowSize() {
            return shallowSize;
        }
        public long getRetainedSize() {
            return retainedSize;
        }
        public String getContextClassLoader() {
            return contextClassLoader;
        }
        public boolean hasStack() {
            return hasStack;
        }
        public boolean isDaemon() {
            return daemon;
        }
        public String getHeapId() {
            return heapId;
        }
        public long getCreatedAt() {
            return createdAt;
        }

        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
            this.getObjectId(), this.getObject(), this.getName(), this.getShallowSize(), this.getRetainedSize(), this.getContextClassLoader(), this.hasStack(), this.isDaemon(), this.getHeapId(), this.getCreatedAt());
        }

        public String uploadSQLStatement() {
            return String.format("INSERT INTO thread_info (heap_id, object_id, object_label, thread_name, context_class_loader, has_stack, is_daemon, shallow_size, retained_size, created_at) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s);", heapId, this.getObjectId(), this.getObject(), this.getName(), this.getContextClassLoader(), this.hasStack(), this.isDaemon(), this.getShallowSize(), this.getRetainedSize(), this.getCreatedAt());
        }
            
    }

