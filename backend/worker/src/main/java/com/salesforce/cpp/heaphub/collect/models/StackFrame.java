package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

public class StackFrame {
        // this needs to be distinguished from outbounds
        // what is object_id, object_label, prefix, suffiz, hasinbound, hasoutbound, shallowsize, retainedsize, gcroot - is this for outbounds
        private String heapId;
        private int threadId;
        private long createdAt;
        private String stack; // done
        private boolean hasLocal; // done
        private boolean firstNonNativeFrame; // done

        public StackFrame(JsonObject obj, int threadId, String heapName, long analysisTime) {
            this.stack = obj.getString(Constant.StackFrame.STACK_NAME_KEY);
            this.hasLocal = obj.getBoolean(Constant.StackFrame.HAS_LOCAL_KEY);
            this.firstNonNativeFrame = obj.getBoolean(Constant.StackFrame.FIRST_NON_NATIVE_FRAME_KEY);
            this.threadId = threadId;
            this.heapId = heapName;
            this.createdAt = analysisTime;
        }
        
        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s\n",
            this.getStack(), this.hasLocal(), this.isFirstNonNativeFrame(), this.getHeapName(), this.getThreadId(), this.isFirstNonNativeFrame());
        }

        public String getStack() {
            return stack;
        }
        public boolean hasLocal() {
            return hasLocal;
        }
        public long getCreatedAt() {
            return createdAt;
        }
        public String getHeapName() {
            return heapId;
        }
        public int getThreadId() {
            return threadId;
        }
        public boolean isFirstNonNativeFrame() {
            return firstNonNativeFrame;
        }

        public String uploadSQLStatement() {
            return String.format("INSERT INTO thread_stack (heap_id, thread_id, stack, has_local, first_non_native_frame, created_at) VALUES (%s, %s, %s, %s, %s, %s);", this.getHeapName(), this.getThreadId(), this.getStack(), this.hasLocal(), this.isFirstNonNativeFrame(), this.getCreatedAt());
        }
    }
    
