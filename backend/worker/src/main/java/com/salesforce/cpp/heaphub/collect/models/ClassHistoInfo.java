package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

public class ClassHistoInfo {
        private int heapId; // done
        private Long createdAt; //
        private String label; // do
        private long numberOfObjects; //
        private long shallowSize; //
        private long retainedSize; //
        private int objectId; // done
        private int type; //


        public ClassHistoInfo(JsonObject obj, int heapId, long analysisTime) {
            String label = obj.getString(Constant.Histogram.LABEL_KEY);
            this.label = label;
            this.numberOfObjects = obj.getLong(Constant.Histogram.NUM_OBJECTS_KEY);
            this.shallowSize = obj.getLong(Constant.Histogram.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Histogram.RETAINED_SIZE_KEY);
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

        public static String uploadSQLStatement() {
            return new String("INSERT INTO histogram (heap_id, object_id, object_label, number_of_objects, object_type, shallow_size, retained_size, created_at, updated_at) VALUES ");
        }

        public String getSQLValues() {
            return String.format("(%s, %s,$HEAPHUB_ESC_TAG$%s$HEAPHUB_ESC_TAG$, %s, %s, %s, %s, to_timestamp(%s), to_timestamp(%s))", 
                    heapId, objectId, label, numberOfObjects, type, shallowSize, retainedSize, createdAt/1000, createdAt/1000);
        }

        static public String[] getCSVHeader() {
            return new String[] {
                    "heap_id",
                    "object_id",
                    "object_label",
                    "number_of_objects",
                    "object_type",
                    "shallow_size",
                    "retained_size",
                    "created_at",
                    "updated_at"
            };
        }

        static public String uploadCSV(String path) {
            return String.format("COPY histogram (heap_id, object_id, object_label, number_of_objects, object_type, shallow_size, retained_size, created_at, updated_at) FROM '%s' DELIMITER ',' CSV HEADER", path);
        }

        public String[] getCSVArray() {
            return new String[] {
                    String.valueOf(heapId),
                    String.valueOf(objectId),
                    label,
                    String.valueOf(numberOfObjects),
                    String.valueOf(type),
                    String.valueOf(shallowSize),
                    String.valueOf(retainedSize),
                    String.format("to_timestamp(%s)", createdAt/1000),
                    String.format("to_timestamp(%s)", createdAt/1000),
            };
        }

}
