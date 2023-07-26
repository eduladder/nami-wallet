package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;


public class ThreadStackLocal {
    private int threadId;
    private int threadInfoId;
    private String stack;
    private boolean hasLocal;
    private boolean firstNonNativeFrame;
    private Integer objectId;
    private String objectLabel;
    private String prefix;
    private String suffix;
    private boolean hasInbound;
    private boolean hasOutbound;
    private Long shallowSize;
    private Long retainedSize;
    private boolean isGCRoot;
    private long createdAt;
    private int heapId;
    private int depth;

    public ThreadStackLocal(int heapId, long createdAt, int threadId, int threadInfoId, int depth) {
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.threadId = threadId;
        this.depth = depth;
        this.threadInfoId = threadInfoId;
    }

    public int getThreadInfoId() {
        return threadInfoId;
    }

    // setters and getters
    public int getDepth() {
        return depth;
    }

    public int getHeapId(){
        return heapId;
    }

    public int getThreadId() {
        return threadId;
    }
    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }
    public String getStack() {
        return stack;
    }
    public void setStack(String stack) {
        this.stack = stack;
    }
    public Boolean hasLocal() {
        return hasLocal;
    }
    public void setHasLocal(Boolean hasLocal) {
        this.hasLocal = hasLocal;
    }
    public boolean isFirstNonNativeFrame() {
        return firstNonNativeFrame;
    }
    public void setFirstNonNativeFrame(boolean firstNonNativeFrame) {
        this.firstNonNativeFrame = firstNonNativeFrame;
    }
    public Integer getObjectId() {
        return objectId;
    }
    public void setObjectId(Integer objectId) {
        this.objectId = objectId;
    }
    public String getPrefix() {
        return prefix;
    }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    public String getSuffix() {
        return suffix;
    }
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
    public boolean hasInbound() {
        return hasInbound;
    }
    public void setHasInbound(boolean hasInbound) {
        this.hasInbound = hasInbound;
    }
    public boolean hasOutbound() {
        return hasOutbound;
    }
    public void setHasOutbound(boolean hasOutbound) {
        this.hasOutbound = hasOutbound;
    }
    public Long getRetainedSize() {
        return retainedSize;
    }
    public void setRetainedSize(Long retainedSize) {
        this.retainedSize = retainedSize;
    }
    public Long getShallowSize() {
        return shallowSize;
    }
    public void setShallowSize(Long shallowSize) {
        this.shallowSize = shallowSize;
    }
        
    public boolean isGCRoot() {
        return isGCRoot;
    }
    public void setGCRoot(boolean isGCRoot) {
        this.isGCRoot = isGCRoot;
    
    }

    public String getObjectLabel() {
        return objectLabel;
    }
    public void setObjectLabel(String objectLabel) {
        this.objectLabel = objectLabel;
    }

    public String toString() {
        return "ThreadStackLocal{" +
                "heapId='" + heapId + '\'' +
                ", createdAt=" + createdAt +
                ", threadId=" + threadId +
                ", threadInfoId=" + threadInfoId +
                ", depth=" + depth +
                ", stack='" + stack + '\'' +
                ", firstNonNativeFrame=" + firstNonNativeFrame +
                ", hasLocal=" + hasLocal +
                ", hasOutbound=" + hasOutbound +
                ", retainedSize=" + retainedSize +
                ", shallowSize=" + shallowSize +
                ", isGCRoot=" + isGCRoot +
                ", objectLabel='" + objectLabel + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                ", hasInbound=" + hasInbound +
                ", objectId=" + objectId +
                '}';
    }

    public ThreadStackLocal copy() {
        ThreadStackLocal copy = new ThreadStackLocal(heapId, createdAt, threadId, threadInfoId, depth);
        copy.setStack(stack);
        copy.setFirstNonNativeFrame(firstNonNativeFrame);
        copy.setHasLocal(hasLocal);
        copy.setObjectId(this.objectId);
        copy.setObjectLabel(this.objectLabel);
        copy.setPrefix(this.prefix);
        copy.setSuffix(this.suffix);
        copy.setHasInbound(this.hasInbound);
        copy.setHasOutbound(this.hasOutbound);
        copy.setRetainedSize(this.retainedSize);
        copy.setShallowSize(this.shallowSize);
        copy.setGCRoot(this.isGCRoot);
        return copy;
    }

    public void addStackInfo(JsonObject obj) {
            this.stack = obj.getString(Constant.StackFrame.STACK_NAME_KEY);
            this.hasLocal = obj.getBoolean(Constant.StackFrame.HAS_LOCAL_KEY);
            this.firstNonNativeFrame = obj.getBoolean(Constant.StackFrame.FIRST_NON_NATIVE_FRAME_KEY);
    }

    public void addLocalsInfo (JsonObject obj) {
        this.objectId = obj.getInteger(Constant.Locals.OBJECT_ID_KEY);
        this.prefix = obj.getString(Constant.Locals.PREFIX_KEY);
        this.suffix = obj.getString(Constant.Locals.SUFFIX_KEY);
        this.shallowSize = obj.getLong(Constant.Locals.SHALLOW_SIZE_KEY);
        this.retainedSize = obj.getLong(Constant.Locals.RETAINED_SIZE_KEY);
        this.hasInbound = obj.getBoolean(Constant.Locals.HAS_INBOUND_KEY);
        this.hasOutbound = obj.getBoolean(Constant.Locals.HAS_OUTBOUND_KEY);
        this.isGCRoot = obj.getBoolean(Constant.Locals.GC_ROOT_KEY);
        this.objectLabel = obj.getString(Constant.Locals.LABEL_KEY);
    }

    public static String uploadSQLStatement() {
        return "INSERT INTO thread_stack (heap_id,thread_info_id, stack, has_local, first_non_native_frame, object_id, object_label, prefix, suffix, has_inbound, has_outbound, retained_size, shallow_size, gc_root, created_at, updated_at) VALUES ";
    }

    public String getSQLValues() {
        return String.format("(%s, %s, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, %s, %s, %s, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, %s, %s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s))", heapId, this.getThreadInfoId(), this.getStack(), this.hasLocal(), this.isFirstNonNativeFrame(), this.getObjectId(), this.getObjectLabel(), this.getPrefix(), this.getSuffix(), this.hasInbound(), this.hasOutbound(), this.getRetainedSize(), this.getShallowSize(), this.isGCRoot(), this.createdAt/1000, this.createdAt/1000).replaceAll("null", "NULL");
    }

    public String[] getCSVArray() {
        return new String[] {
            String.valueOf(heapId),
            String.valueOf(this.getThreadInfoId()),
            this.getStack(),
            String.valueOf(this.hasLocal()),
            String.valueOf(this.isFirstNonNativeFrame()),
            String.valueOf(this.getObjectId()),
            this.getObjectLabel(),
            this.getPrefix(),
            this.getSuffix(),
            String.valueOf(this.hasInbound()),
            String.valueOf(this.hasOutbound()),
            String.valueOf(this.getRetainedSize()),
            String.valueOf(this.getShallowSize()),
            String.valueOf(this.isGCRoot()),
            String.format("to_timestamp(%s)", this.createdAt/1000),
            String.format("to_timestamp(%s)", this.createdAt/1000)
        };
    }

    public static String[] getCSVHeader() {
        return new String[]{
                "heap_id",
                "thread_info_id",
                "stack",
                "has_local",
                "first_non_native_frame",
                "object_id",
                "object_label",
                "prefix",
                "suffix",
                "has_inbound",
                "has_outbound",
                "retained_size",
                "shallow_size",
                "gc_root",
                "created_at",
                "updated_at"
        };
    }

    public static String uploadCSV(String path) {
        return String.format("COPY thread_stack (heap_id,thread_info_id, stack, has_local, first_non_native_frame, object_id, object_label, prefix, suffix, has_inbound, has_outbound, retained_size, shallow_size, gc_root, created_at, updated_at) FROM '%s' DELIMITER ',' CSV HEADER", path);
    }
    
}
