package com.salesforce.cpp.heaphub.collect.models;

import io.vertx.core.json.JsonObject;
import org.eclipse.jifa.worker.Constant;

public class Outbounds {
        private int objectId;
        private String prefix;
        private String label;
        private String suffix;
        private long shallowSize;
        private long retainedSize;
        private boolean hasInbound;
        private boolean hasOutbound;
        private int objectType;
        private boolean gCRoot;
        private int sourceId; 
        private int heapId;
        private long createdAt;

        public Outbounds(JsonObject obj, int sourceId, int heapId, long analysisTime) {
            this.objectId = obj.getInteger(Constant.Outbounds.OBJECT_ID_KEY);
            this.prefix = obj.getString(Constant.Outbounds.PREFIX_KEY);
            this.label = obj.getString(Constant.Outbounds.LABEL_KEY);
            this.suffix = obj.getString(Constant.Outbounds.SUFFIX_KEY);
            this.shallowSize = obj.getLong(Constant.Outbounds.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Outbounds.RETAINED_SIZE_KEY);
            this.hasInbound = obj.getBoolean(Constant.Outbounds.HAS_INBOUND_KEY);
            this.hasOutbound = obj.getBoolean(Constant.Outbounds.HAS_OUTBOUND_KEY);
            this.objectType = obj.getInteger(Constant.Outbounds.OBJECT_TYPE_KEY);
            this.gCRoot = obj.getBoolean(Constant.Outbounds.GC_ROOT_KEY);
            this.sourceId = sourceId;
            this.heapId = heapId;
            this.createdAt = analysisTime;
        }

        public int getObjectId() {
            return objectId;
        }

        public String getPrefix() {
            return prefix;
        }
        public String getLabel() {
            return label;
        }
        public String getSuffix() {
            return suffix;
        }
        public long getShallowSize() {
            return shallowSize;
        }
        public long getRetainedSize() {
            return retainedSize;
        }
        public boolean isHasInbound() {
            return hasInbound;
        }
        public boolean isHasOutbound() {
            return hasOutbound;
        }
        public int getObjectType() {
            return objectType;
        }
        public boolean isGCRoot() {
            return gCRoot;
        }
        public int getSourceId() {
            return sourceId;
        }
        public int getHeapId() {
            return heapId;
        }
        public long getCreatedAt() {
            return createdAt;
        }
        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", objectId, prefix, label, suffix, shallowSize, retainedSize, hasInbound, hasOutbound, objectType, gCRoot, sourceId, heapId, createdAt);
        }

        public static String uploadSQLStatement() {
            return "INSERT INTO outbounds (heap_id,source_id, object_id, prefix, label, suffix, shallow_size, retained_size, has_inbound, has_outbound, object_type, gc_root, created_at, updated_at) VALUES ";
        }

        public String getSQLValues() {
            return String.format("(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,to_timestamp(%s),to_timestamp(%s))", heapId, sourceId, objectId, prefix, label, suffix, shallowSize, retainedSize, hasInbound, hasOutbound, objectType, gCRoot, createdAt/1000, createdAt/1000);
        }

        public String[] getCSVArray() {
            return new String[] {
                    String.valueOf(heapId),
                    String.valueOf(sourceId),
                    String.valueOf(objectId),
                    prefix,
                    label,
                    suffix,
                    String.valueOf(shallowSize),
                    String.valueOf(retainedSize),
                    String.valueOf(hasInbound),
                    String.valueOf(hasOutbound),
                    String.valueOf(objectType),
                    String.valueOf(gCRoot),
                    String.format("to_timestamp(%s)", createdAt/1000),
                    String.format("to_timestamp(%s)", createdAt/1000),
            };
        }

        public static String[] getCSVHeader () {
            return new String[] {
                "heap_id",
                "source_id",
                "object_id",
                "prefix",
                "label",
                "suffix",
                "shallow_size",
                "retained_size",
                "has_inbound",
                "has_outbound",
                "object_type",
                "gc_root",
                "created_at",
                "updated_at",
            };
        }

        public static String uploadCSV(String path) {
            return String.format("COPY outbounds (heap_id,source_id, object_id, prefix, label, suffix, shallow_size, retained_size, has_inbound, has_outbound, object_type, gc_root, created_at, updated_at) FROM '%s' DELIMITERS ',' CSV HEADER;", path);
        }

    }
    