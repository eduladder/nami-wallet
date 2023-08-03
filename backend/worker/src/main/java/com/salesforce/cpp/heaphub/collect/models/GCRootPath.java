package com.salesforce.cpp.heaphub.collect.models;

import java.util.List;

import io.vertx.core.json.JsonObject;

/***
 * Model to represent patch to garbage collection root
 */
public class GCRootPath {
            
    static public class PathToGCRootElement {
        private int objectId;
        private int parentId;
        private String label;
        private String memoryLocation;
        private boolean origin;
        private String prefix;
        private String suffix;
        private int shallowSize;
        private int retainedSize;
        private boolean hasInbound;
        private boolean hasOutbound;
        private int objectType;
        private boolean gCRoot;
        private long createdAt;
        private int sourceId;
        private int heapId;

        /**
         * Constructor to create GCRootPath object from a JsonObject returned from JIFA backend. Assumes a valid JsonObject
         * @param obj 
         * @param parentId parent of current GC Root element, -1 if root
         * @param sourceId id of element that we are trying to find the path to GC root for
         * @param heapId primary key of heap in SQL database
         * @param analysisTime time when analysis is being conducted
         */
        public PathToGCRootElement(JsonObject obj, int parentId, int sourceId, int heapId, long analysisTime) {
            if (obj.getString("suffix") == null) {
                suffix = "";
            } else {
                suffix = obj.getString("suffix");
            }

            if (obj.getString("prefix") == null) {
                prefix = "";
            } else {
                prefix = obj.getString("prefix");
            }

            if (obj.getBoolean("hasInbound") == null) {
                hasInbound = false;
            } else {
                hasInbound = obj.getBoolean("hasInbound");
            }

            if (obj.getBoolean("hasOutbound") == null) {
                hasOutbound = false;
            } else {
                hasOutbound = obj.getBoolean("hasOutbound");
            }

            if (obj.getBoolean("GCRoot") == null) {
                gCRoot = false;
            } else {
                gCRoot = obj.getBoolean("GCRoot");
            }

            this.parentId = parentId;
            objectId = obj.getInteger("objectId");
            label = obj.getString("label");
            shallowSize = obj.getInteger("shallowSize");
            retainedSize = obj.getInteger("retainedSize");
            objectType = obj.getInteger("objectType");
            this.sourceId = sourceId;
            createdAt = analysisTime;
            this.heapId = heapId;
        }
        
        public int getObjectId() {
            return objectId;
        }

        public int parentId() {
            return parentId;
        }

        public String label() {
            return label;
        }
        
        public String memoryLocation() {
            return memoryLocation;
        }
        
        public boolean origin() {
            return origin;
        }
        
        public String suffix() {
            return suffix;
        }
        
        public String prefix() {
            return prefix;
        }
        
        public boolean hasInbound() {
            return hasInbound;
        }
        
        public boolean hasOutbound() {
            return hasOutbound;
        }
        
        public boolean gCRoot() {
            return gCRoot;
        }
        
        public int shallowSize() {
            return shallowSize;
        }
        
        public int retainedSize() {
            return retainedSize;
        }
        
        public int objectType() {
            return objectType;
        }
        
        public int heapId() {
            return heapId;
        }

        public long createdAt() {
            return createdAt;
        }

        public int sourceId() {
            return sourceId;
        }

        /**
         * Constructs the SQL header for a batch insert
         * @return string of header for batch insert
         */
        public static String uploadSQLStatement() {
            return "INSERT INTO path_to_gc_root (heap_id, source_id, parent_id, object_id, label, memory_location, origin, suffix, prefix, has_inbound, has_outbound, gc_root, shallow_size, retained_size, object_type, created_at, updated_at) VALUES ";
        }

        /**
         * Constructs the SQL values for a batch insert
         * @return sql values for batch instert as string
         */
        public String getSQLValues() {
            return String.format("(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s))",
            heapId, sourceId, parentId, objectId, label, memoryLocation, origin, suffix, prefix, hasInbound, hasOutbound, gCRoot, shallowSize, retainedSize, objectType, createdAt/1000, createdAt/1000);
        }
    }

    private List<PathToGCRootElement> path;
    private boolean hasMore;

    /**
     * Constructor to create GCRootPath object a list of PathToGCRootElement.
     * @param path - list of PathToGCRootElement
     * @param hasMore - whether or not this path has more elements in it
     */
    public GCRootPath(List<PathToGCRootElement> path, boolean hasMore) {
        this.path = path;
        this.hasMore = hasMore;
    }
    
    public List<PathToGCRootElement> getPath() {
        return path;
    }
    
    public boolean hasMore() {
        return hasMore;
    }
    }

