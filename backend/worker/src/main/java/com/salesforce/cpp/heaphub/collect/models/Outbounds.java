package com.salesforce.cpp.heaphub.collect.models;

import io.vertx.core.json.JsonObject;
import org.eclipse.jifa.worker.Constant;


/**
 * Model to represent Outbound information for Dominator Tree
 */
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

        /**
         * Constructor to create Outbounds object from a JsonObject returned from JIFA backend. Assumes a valid JsonObject
         * @param obj
         * @param sourceId root for which the oubound is being generated
         * @param heapId primay key of heap in SQL database
         * @param analysisTime time when analysis is being conducted
         */
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

        /***
         * Get the SQL header to batch insert a new Outbound into the database
         * @return sql header
         */
        public static String uploadSQLStatement() {
            return "INSERT INTO outbounds (heap_id,source_id, object_id, prefix, label, suffix, shallow_size, retained_size, has_inbound, has_outbound, object_type, gc_root, created_at, updated_at) VALUES ";
        }

        /**
         * Get the SQL values to batch insert a new Outbound into the database
         * @return sql values string
         */
        public String getSQLValues() {
            return String.format("(%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,to_timestamp(%s),to_timestamp(%s))", heapId, sourceId, objectId, prefix, label, suffix, shallowSize, retainedSize, hasInbound, hasOutbound, objectType, gCRoot, createdAt/1000, createdAt/1000);
        }

    }
    