package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

/**
 * Model to represent Class Histogram Information
 */
public class ClassHistoInfo {
        private int heapId;
        private Long createdAt;
        private String label;
        private long numberOfObjects;
        private long shallowSize;
        private long retainedSize;
        private int objectId;
        private int type;

        /**
         * Constructor to create ClassHistoInfo object from a JsonObject returned from JIFA backend. Assumes a valid JsonObject
         * @param obj
         * @param heapId primary key of heap in SQL database
         * @param analysisTime time taken to generate the histogram
         */
        public ClassHistoInfo(JsonObject obj, int heapId, long analysisTime) {
            String label = obj.getString(Constant.Histogram.LABEL_KEY);
            this.label = label;
            this.numberOfObjects = obj.getLong(Constant.Histogram.NUM_OBJECTS_KEY);
            this.shallowSize = obj.getLong(Constant.Histogram.SHALLOW_SIZE_KEY);
            // for some reason backend returns negative number
            // fixing in backend might affect frontend, so for now just negate retained size
            this.retainedSize = -obj.getLong(Constant.Histogram.RETAINED_SIZE_KEY);
            this.objectId = obj.getInteger(Constant.Histogram.OBJECT_ID_KEY);
            this.type = obj.getInteger(Constant.Histogram.TYPE_KEY);
            this.createdAt = analysisTime;
            this.heapId = heapId;
        }


        public String getLabel() {
            return label;
        }
        public long getNumberOfObjects() {
            return numberOfObjects;
        }
        public long getShallowSize() {
            return shallowSize;
        }
        public long getRetainedSize() {
            return retainedSize;
        }
        public int getObjectId() {
            return objectId;
        }
        public int getType() {
            return type;
        }

        public int getHeapId() {
            return heapId;
        }
        public Long getCreatedAt() {
            return createdAt;
        }

        public String toString() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                    heapId, objectId, label, numberOfObjects, type, shallowSize, retainedSize, createdAt);
        }

        // header for batch insert statement
        public static String uploadSQLStatement() {
            return new String("INSERT INTO histogram (heap_id, object_id, object_label, number_of_objects, object_type, shallow_size, retained_size, created_at, updated_at) VALUES ");
        }

        // values for batch insert statement
        public String getSQLValues() {
            return String.format("(%s, %s,$HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, %s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s))", 
                    heapId, objectId, label, numberOfObjects, type, shallowSize, retainedSize, createdAt/1000, createdAt/1000);
        }

}
