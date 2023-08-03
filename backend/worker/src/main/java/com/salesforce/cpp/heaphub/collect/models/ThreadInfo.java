package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

/**
 * Model to represent Thread Information
 */
public class ThreadInfo {
        private int heapId;
        private long createdAt;
        private int objectId;
        private String objectLabel;
        private String threadName;
        private long shallowSize;
        private long retainedSize;
        private String contextClassLoader;
        private boolean hasStack;
        private boolean daemon;


        /**
         * Constructor to create ThreadInfo object from a JsonObject returned from JIFA backend. Assumes a valid JsonObject
         * @param obj
         * @param heapId primary key of heap in SQL database
          * @param analysisTime time when analysis is being conducted
         */
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


        /**
         * Returns the SQL header to batch insert a ThreadInfo object into the database
         * @return sql header
         */
        public static String uploadSQLStatement() {
            return "INSERT INTO thread_info (heap_id, object_id, object_label, thread_name, context_class_loader, has_stack, is_daemon, shallow_size, retained_size, created_at, updated_at) VALUES ";
        }

         /**
         * Returns the SQL values to batch insert a ThreadInfo object into the database
         * @return sql values
         */
        public String getSQLValues() {
            return String.format("(%s, %s, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, %s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s))", heapId, this.getObjectId(), this.getObjectLabel(), this.getThreadName(), this.getContextClassLoader(), this.hasStack(), this.isDaemon(), this.getShallowSize(), this.getRetainedSize(), this.getCreatedAt()/1000, this.getCreatedAt()/1000);
        }

         /**
         * Returns the SQL statement to retrieve  the thread_info_id (primary key) and object_id for ThreadInfo object. The purpose is to help create the ThreadIds datatype
         * @return sql statement
         */
        public static String getIds(int heapId) {
            return String.format("SELECT thread_info_id, object_id FROM thread_info WHERE heap_id = %s;", heapId);
        }

    }

