package com.salesforce.cpp.heaphub.collect.models;

import org.eclipse.jifa.worker.Constant;

import com.google.gson.JsonObject;

public class HeapSummary {
    String name;
    String generatedName;
    Integer heapId;
    Long usedHeapSize;
    Long classCount;
    Long objectCount;
    Long classLoaderCount;
    Long gcRootCount;
    String osBit;
    String pod;
    String hostName;
    Long heapCreationDate;
    Long createdAt;
    Long updatedAt;
    String jvmParameters;

    public HeapSummary(String name, String generatedName, int heapId, long usedHeapSize, long classCount, long objectCount, long classLoaderCount, long gcRootCount, String osBit, String pod, String hostName, long heapCreationDate, long createdAt, long updatedAt, String jvmParameters) {
        this.name = name;
        this.generatedName = generatedName;
        this.heapId = heapId;
        this.usedHeapSize = usedHeapSize;
        this.classCount = classCount;
        this.objectCount = objectCount;
        this.classLoaderCount = classLoaderCount;
        this.gcRootCount = gcRootCount;
        this.osBit = osBit;
        this.pod = pod;
        this.hostName = hostName;
        this.heapCreationDate = heapCreationDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.jvmParameters = jvmParameters;
    }

    public HeapSummary() {

    }

    // setters and getters
    public String getName() {
        return name;
    }
    
    public String getGeneratedName() {
        return generatedName;
    }
    
    public int getHeapId() {
        return heapId;
    }
    
    public long getUsedHeapSize() {
        return usedHeapSize;
    }
    
    public long getClassCount() {
        return classCount;
    }
    
    public long getObjectCount() {
        return objectCount;
    }
    
    public long getClassLoaderCount() {
        return classLoaderCount;
    }
    
    public long getGcRootCount() {
        return gcRootCount;
    }
    
    public String getOsBit() {
        return osBit;
    }
    
    public String getPod() {
        return pod;
    }
    
    public String getHostName() {
        return hostName;
    }
    
    public long getHeapCreationDate() {
        return heapCreationDate;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setGeneratedName(String generatedName) {
        this.generatedName = generatedName;
    }
    
    public void setHeapId(int heapId) {
        this.heapId = heapId;
    }
    
    public void setUsedHeapSize(long usedHeapSize) {
        this.usedHeapSize = usedHeapSize;
    }
    
    public void setClassCount(long classCount) {
        this.classCount = classCount;
    }
    
    public void setObjectCount(long objectCount) {
        this.objectCount = objectCount;
    }
    
    public void setClassLoaderCount(long classLoaderCount) {
        this.classLoaderCount = classLoaderCount;
    }
    
    public void setGcRootCount(long gcRootCount) {
        this.gcRootCount = gcRootCount;
    }
    
    public void setOsBit(String osBit) {
        this.osBit = osBit;
    }
    
    public void setPod(String pod) {
        this.pod = pod;
    }
    
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    public void setHeapCreationDate(long heapCreationDate) {
        this.heapCreationDate = heapCreationDate;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    } 

    public void setJVMParameters(String jvmParameters) {
        this.jvmParameters = jvmParameters;
    }

    public String getJVMParameters() {
        return jvmParameters;
    }

    public String uploadSQLStatement() {
        return String.format("INSERT INTO heap_summary (name, generated_name, used_heap_size, class_count, object_count, class_loader_count, gc_root_count, os_bit, pod, host_name, heap_creation_date, created_at, updated_at, jvm_parameters) VALUES ('%s', '%s', %s, %s, %s, %s, %s, '%s', '%s', '%s', to_timestamp(%s), to_timestamp(%s), to_timestamp(%s), '%s');", 
        this.name, 
        this.generatedName, 
        this.usedHeapSize, 
        this.classCount, 
        this.objectCount, 
        this.classLoaderCount, 
        this.gcRootCount, 
        this.osBit, 
        this.pod, 
        this.hostName, 
        this.heapCreationDate/1000, 
        this.createdAt/1000, 
        this.updatedAt/1000, 
        this.jvmParameters);
    }

    public String getHeapIdSQL() {
        return String.format("SELECT heap_id FROM heap_summary WHERE name = '%s';", this.name);
    }
    
}
