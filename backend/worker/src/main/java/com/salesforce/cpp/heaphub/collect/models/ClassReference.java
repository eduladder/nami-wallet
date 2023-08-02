package com.salesforce.cpp.heaphub.collect.models;

import org.checkerframework.checker.regex.qual.Regex;
import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

public class ClassReference {
    private int heapId;
    private int objectId;
    private String prefix;
    private String suffix;
    private String objectLabel;
    private boolean isInbound;
    private boolean hasInbound;
    private boolean hasOutbound;
    private long shallowSize;
    private long retainedSize;
    private int objectType;
    private boolean gCRoot;
    private long createdAt;
    private int parentId;

    public ClassReference(JsonObject obj, int heapId, long createdAt, int parentId, boolean isInbound) {
            this.objectId = obj.getInteger(Constant.Outbounds.OBJECT_ID_KEY);
            this.prefix = obj.getString(Constant.Outbounds.PREFIX_KEY);
            this.objectLabel = obj.getString(Constant.Outbounds.LABEL_KEY);
            this.suffix = obj.getString(Constant.Outbounds.SUFFIX_KEY);
            this.shallowSize = obj.getLong(Constant.Outbounds.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Outbounds.RETAINED_SIZE_KEY);
            this.hasInbound = obj.getBoolean(Constant.Outbounds.HAS_INBOUND_KEY);
            this.hasOutbound = obj.getBoolean(Constant.Outbounds.HAS_OUTBOUND_KEY);
            this.objectType = obj.getInteger(Constant.Outbounds.OBJECT_TYPE_KEY);
            this.gCRoot = obj.getBoolean(Constant.Outbounds.GC_ROOT_KEY);
            this.parentId = parentId;
            this.heapId = heapId;
            this.createdAt = createdAt;
            this.isInbound = isInbound;
    }
    /*            "objectId": 754,
            "gCRoot": true */
    
    public int getHeapId() {
        return heapId;
    }
    
    
    public int getObjectId() {
        return objectId;
    }
    
    public String getObjectLabel() {
        return objectLabel;
    }
    
    public boolean isInbound() {
        return isInbound;
    }
    
    public boolean hasInbound() {
        return hasInbound;
    }
    
    public boolean hasOutbound() {
        return hasOutbound;
    }
    
    public long getShallowSize() {
        return shallowSize;
    }
    
    public long getRetainedSize() {
        return retainedSize;
    }
    
    public boolean isGCRoot() {
        return gCRoot;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }

    public int getParentId() {
        return parentId;
    }

    public String getPrefix() {
        return prefix;
    }
    
    public String getSuffix() {
        return suffix;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setHeapId(int heapId) {
        this.heapId = heapId;
    }

    
    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }
    
    public void setObjectLabel(String objectLabel) {
        this.objectLabel = objectLabel;
    }
    
    public void setInbound(boolean isInbound) {
        this.isInbound = isInbound;
    }
    
    public void setHasInbound(boolean hasInbound) {
        this.hasInbound = hasInbound;
    }
    
    public void setHasOutbound(boolean hasOutbound) {
        this.hasOutbound = hasOutbound;
    }
    
    public void setShallowSize(long shallowSize) {
        this.shallowSize = shallowSize;
    }
    
    public void setRetainedSize(long retainedSize) {
        this.retainedSize = retainedSize;
    }
    
    public void setGCRoot(boolean gCRoot) {
        this.gCRoot = gCRoot;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public static String uploadSQLStatement() {
        return "INSERT INTO histogram_reference (heap_id, parent_id, object_id, object_label, prefix, suffix, is_inbound, has_inbound, has_outbound, shallow_size, retained_size, object_type, gc_root, created_at, updated_at) VALUES ";
    }

    public String getSQLValues() {
        return String.format("(%s, %s, %s, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, $HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, %s, %s, %s, %s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s))", 
                heapId, parentId, objectId, objectLabel, prefix, suffix, isInbound, hasInbound, hasOutbound, shallowSize, retainedSize, objectType, gCRoot, createdAt/1000, createdAt/1000).replaceAll("[^\u0001-\u007F]+", "");
    }

}
