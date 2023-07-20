package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

public class ClassHistoInfo {
        private String heapId;
        private Long createdAt;
        private String memLocation;
        private String label;
        private long numberOfObjects; 
        private long shallowSize;
        private long retainedSize;
        private int objectId;
        private int type;


        public ClassHistoInfo(JsonObject obj, String heapName, long analysisTime) {
            String label = obj.getString(Constant.Histogram.LABEL_KEY);
            String[] splitLabel = label.split("@ ");
            if (splitLabel.length != 2) {
                throw new IllegalArgumentException("Invalid label: " + label);
            }
            this.label = splitLabel[0];
            this.memLocation = splitLabel[1];
            this.numberOfObjects = obj.getLong(Constant.Histogram.NUM_OBJECTS_KEY);
            this.shallowSize = obj.getLong(Constant.Histogram.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Histogram.RETAINED_SIZE_KEY);
            this.objectId = obj.getInteger(Constant.Histogram.OBJECT_ID_KEY);
            this.type = obj.getInteger(Constant.Histogram.TYPE_KEY);
            this.createdAt = analysisTime;
            this.heapId = heapName;
        }

        public String getMemLocation() {
            return memLocation;
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

        public String getHeapId() {
            return heapId;
        }
        public Long getCreatedAt() {
            return createdAt;
        }

        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
            this.getLabel(), this.getMemLocation(), this.getObjectId(), this.getRetainedSize(), this.getShallowSize(), this.getNumberOfObjects(), this.getType(), this.getHeapId(), this.getCreatedAt());
        }

        public String uploadSQLStatement() {
            return "INSERT INTO histogram (heap_id, object_id, object_label, number_of_objects, object_type, shallow_size, retained_size, created_at VALUES (" + this.getHeapId() + ", " + this.getObjectId() + ", " + this.getLabel() + ", " + this.getNumberOfObjects() + ", " + this.getType() + ", " + this.getShallowSize() + ", " + this.getRetainedSize() + ", " + this.getCreatedAt() + ");";
        }

}
