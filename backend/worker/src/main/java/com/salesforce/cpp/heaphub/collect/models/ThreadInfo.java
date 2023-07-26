package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

public class ThreadInfo {
        private int heapId;
        private long createdAt;
        private int objectId; // done
        private String objectLabel; // done object_label
        private String threadName; // done
        private long shallowSize; // done
        private long retainedSize; // done
        private String contextClassLoader; // done
        private boolean hasStack; // done
        private boolean daemon; // done

        public ThreadInfo(JsonObject obj, int heapId, long analysisTime) {
            this.objectId = obj.getInteger(Constant.Threads.OBJECT_ID_KEY);
            this.objectLabel = obj.getString(Constant.Threads.OBJECT_KEY);
            this.threadName = obj.getString(Constant.Threads.NAME_KEY);
            this.shallowSize = obj.getLong(Constant.Threads.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Threads.RETAINED_SIZE_KEY);
            this.contextClassLoader = obj.getString(Constant.Threads.CONTEXT_CLASS_LOADER_KEY);
            this.hasStack = obj.getBoolean(Constant.Threads.HAS_STACK_KEY);
            this.daemon = obj.getBoolean(Constant.Threads.DAEMON_KEY);
            this.heapId = heapId;
            createdAt = analysisTime;
        }

        public int getObjectId() {
            return objectId;
        }

        public String getObjectLabel() {
            return objectLabel;
        }
        public String getThreadName() {
            return threadName;
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
        public int getHeapId() {
            return heapId;
        }
        public long getCreatedAt() {
            return createdAt;
        }

        public static String uploadSQLStatement() {
            return "INSERT INTO thread_info (heap_id, object_id, object_label, thread_name, context_class_loader, has_stack, is_daemon, shallow_size, retained_size, created_at, updated_at) VALUES ";
        }


        public String getSQLValues() {
            return String.format("(%s, %s, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, %s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s))", heapId, this.getObjectId(), this.getObjectLabel(), this.getThreadName(), this.getContextClassLoader(), this.hasStack(), this.isDaemon(), this.getShallowSize(), this.getRetainedSize(), this.getCreatedAt()/1000, this.getCreatedAt()/1000);
        }

        public static String getIds(int heapId) {
            return String.format("SELECT thread_info_id, object_id FROM thread_info WHERE heap_id = %s;", heapId);
        }

        public String[] getCSVArray() {
            return new String[] { 
                    String.valueOf(this.getHeapId()),
                    String.valueOf(this.getObjectId()),
                    this.getObjectLabel(),
                    this.getThreadName(),
                    this.getContextClassLoader(),
                    String.valueOf(this.hasStack()),
                    String.valueOf(this.isDaemon()),
                    String.valueOf(this.getShallowSize()),
                    String.valueOf(this.getRetainedSize()),
                    String.format("to_timestamp(%s)", this.getCreatedAt()/1000),
                    String.format("to_timestamp(%s)", this.getCreatedAt()/1000)
            };
        }

        public static String[] getCSVHeader() {
            return new String[] {
                    "heap_id",
                    "object_id",
                    "object_label",
                    "thread_name",
                    "context_class_loader",
                    "has_stack",
                    "is_daemon",
                    "shallow_size",
                    "retained_size",
                    "created_at",
                    "updated_at"
            };
        }

        public static String uploadCSV(String path) {
            return String.format("COPY thread_info (thread_info (heap_id, object_id, object_label, thread_name, context_class_loader, has_stack, is_daemon, shallow_size, retained_size, created_at, updated_at) FROM '%s' DELIMITERS ',' CSV HEADER;", path);
        }

    }

