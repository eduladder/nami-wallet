package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

public class DomTreeObject {
        private int parentId; // done
        private String memLocation; // done
        private long createdAt;
        private int heapId;
        private boolean isDomRoot;
        private String label;
        private String suffix;
        private int objectId;
        private int objectType;
        private boolean gCRoot;
        private long shallowSize;
        private long retainedSize;
        private double percent;
        private Boolean hasInbound;
        private Boolean hasOutbound;
        private String prefix;
        // has inbound and has outbound are not present in returned json
        // prefix does not exist
        // private double percent; // not included in the stored information

        DomTreeObject(JsonObject obj) {
            gCRoot = (obj.getBoolean(Constant.DomTree.GC_ROOT_KEY));
            label = (obj.getString(Constant.DomTree.LABEL_KEY));
            objectId = (obj.getInteger(Constant.DomTree.OBJECT_ID_KEY));
            objectType = (obj.getInteger(Constant.DomTree.OBJECT_TYPE_KEY));
            percent = (obj.getDouble(Constant.DomTree.PERCENT_KEY));
            retainedSize = (obj.getLong(Constant.DomTree.RETAINED_SIZE_KEY));
            shallowSize = (obj.getLong(Constant.DomTree.SHALLOW_SIZE_KEY));
            suffix = (obj.getString(Constant.DomTree.SUFFIX_KEY));
            String collectedLabel = obj.getString(Constant.DomTree.LABEL_KEY);
            String[] splitLabel = collectedLabel.split("@ ");
            if (splitLabel.length != 2) {
                throw new IllegalArgumentException("Invalid label: " + collectedLabel);
            }
            this.label = (splitLabel[0]);
            this.setMemLocation(splitLabel[1]);
        }

        public DomTreeObject(JsonObject obj, int parentId, boolean isRoot, long analysisTime, int heapId) {
            this(obj);
            this.parentId = parentId;
            this.createdAt = analysisTime;
            this.heapId = heapId;
            this.isDomRoot = isRoot;
        }

        public void setParentId(int parentId) {
            this.parentId = parentId;
        }

        public void setMemLocation(String memLocation) {
            this.memLocation = memLocation;
        }

        public int getParentId() {
            return parentId;
        }

        public String getMemLocation() {
            return memLocation;
        }

        public long getCreatedAt() {
            return createdAt;
        }
        
        public int getHeapId() {
            return heapId;
        }
        
        public boolean isDomRoot() {
            return isDomRoot;
        }

        public boolean isGCRoot() {
            return gCRoot;
        }
        
        public String getLabel() {
            return label;
        }
        
        public int getObjectId() {
            return objectId;
        }
        
        public int getObjectType() {
            return objectType;
        }
        
        public double getPercent() {
            return percent;
        }
        
        public long getRetainedSize() {
            return retainedSize;
        }
        
        public long getShallowSize() {
            return shallowSize;
        }
        
        public String getSuffix() {
            return suffix;
        }

        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s, %s,%s,%s\n",
            this.getLabel(), this.getMemLocation(), this.getObjectId(), this.getObjectType(), this.getParentId(), this.getPercent(), this.getRetainedSize(), this.getShallowSize(), this.getSuffix(), this.isGCRoot(), this.createdAt, this.heapId, this.isDomRoot);
        }

        // percent is not included - add it 
        public static String uploadSQLStatement() {
            return "INSERT INTO dominator_tree (heap_id, object_id, parent_id, object_label, memory_location, origin, suffix, shallow_size, retained_size, object_type, gc_root, created_at, updated_at, prefix, has_inbound, has_outbound) VALUES ";
        }

        public String getSQLValues() {
            return ("(" + heapId + ", " + this.getObjectId() + ", " + parentId + ", $TAG_24121$" + this.getLabel() + "$TAG_24121$, $TAG_24121$" + this.getMemLocation() + "$TAG_24121$, " + this.isDomRoot + ", $TAG_24121$" + this.getSuffix() + "$TAG_24121$, " + this.getShallowSize() + ", " + this.getRetainedSize() + ", " + this.getObjectType() + ", " + this.isGCRoot() + ", to_timestamp(" + this.getCreatedAt()/1000 + "), to_timestamp(" + this.getCreatedAt()/1000 + "), NULL, NULL, NULL)");
        }

        public String[] getCSVArray() {
            return new String[] {
                String.valueOf(heapId),
                String.valueOf(this.getObjectId()),
                String.valueOf(parentId),
                this.getLabel(),
                this.getMemLocation(),
                String.valueOf(this.isDomRoot),
                String.valueOf(this.getSuffix()),
                String.valueOf(this.getShallowSize()),
                String.valueOf(this.getRetainedSize()),
                String.valueOf(this.getObjectType()),
                String.valueOf(this.getObjectId()),
                String.valueOf(this.isGCRoot()),
                String.format( "to_timestamp(%d)", this.getCreatedAt()/1000),
                String.format( "to_timestamp(%d)", this.getCreatedAt()/1000),
                "NULL",
                "NULL",
                "NULL",
                String.valueOf(this.getPercent())
            };
        }

        static public String[] getCSVHeader() {
            return new String[] {
                "heap_id",
                "object_id",
                "parent_id",
                "object_label",
                "memory_location",
                "origin",
                "suffix",
                "shallow_size",
                "retained_size",
                "object_type",
                "gc_root",
                "created_at",
                "updated_at",
                "prefix",
                "has_inbound",
                "has_outbound",
                "percent"
            };
        }

        static public String uploadCSV(String path) {
            return String.format("COPY dominator_tree (heap_id, object_id, parent_id, object_label, memory_location, origin, suffix, shallow_size, retained_size, object_type, gc_root, created_at, updated_at, prefix, has_inbound, has_outbound, percent) FROM '%s' DELIMITER ',' CSV HEADER", path);
        }

}
