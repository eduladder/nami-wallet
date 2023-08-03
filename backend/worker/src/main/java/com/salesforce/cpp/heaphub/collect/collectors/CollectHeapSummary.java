package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.eclipse.jifa.worker.Constant;
import org.json.JSONArray;

import com.salesforce.cpp.heaphub.collect.models.HeapSummary;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonObject;


/**
 * Collector class to collect all basic heap information for a given heap dump.
 */
public class CollectHeapSummary extends CollectBase {

    String generatedName;
    String originalName;
    long createdAt;
    long heapCreationDate;


    /**
     * Constructor
     * @param originalName original name of the heap
      * @param generatedName generated name of the heap
      * @param heapCreationDate time when heap was created
       * @param createdAt time when analysis was conducted
     */
    public CollectHeapSummary (String originalName, String generatedName, long heapCreationDate, long createdAt) {
        this.originalName = originalName;
        this.generatedName = generatedName;
        this.heapCreationDate = heapCreationDate;
        this.createdAt = createdAt;
    }
    
    /***
     * Make a request to JIFA API to get the heap details
     * @return HeapSummary
      * @throws ClientProtocolException
       * @throws IOException
     */
    public HeapSummary getHeapDetails() throws ClientProtocolException, IOException {
        HttpGet getSummary = new HttpGet(Constant.API.HEAP_DUMP_API_PREFIX + "/" + generatedName + "/details"); 
        Response res = new Response(CLIENT_SYNC.execute(getSummary));
        if (res.getStatusCode() >= 300) {
            log("Request Failed");
            return null;
        }
        JsonObject resJSON = res.getBody();
        if (resJSON == null) {
            log("jsonArray null");
            return null;
        }
        HeapSummary hs = createHeapSummary(resJSON);        
        return hs;
    }

    /**
     * Helper function to create a HeapSummary object from the JsonObject
     * @param obj JsonObject to convert
     * @return
     */
    HeapSummary createHeapSummary(JsonObject obj) {
        HeapSummary out = new HeapSummary();
        out.setUsedHeapSize(obj.getLong(Constant.HeapSummary.USED_HEAP_SIZE_KEY));
        out.setClassCount(obj.getLong(Constant.HeapSummary.NUMBER_OF_CLASSES_KEY));
        out.setObjectCount(obj.getLong(Constant.HeapSummary.NUMBER_OF_OBJECTS_KEY));
        out.setClassLoaderCount(obj.getLong(Constant.HeapSummary.NUMBER_OF_CLASS_LOADERS_KEY));
        out.setGcRootCount(obj.getLong(Constant.HeapSummary.NUMBER_OF_GC_ROOTS_KEY));
        out.setName(originalName);
        out.setCreatedAt(createdAt);
        out.setUpdatedAt(createdAt);
        out.setGeneratedName(generatedName);
        out.setHeapCreationDate(heapCreationDate);
        out.addHostAndPod(originalName);
        return out;
    }


    /**
     * Collect the heap summary and upload it to SQL
     * @return int heapId of the heap summary
     * @throws ClientProtocolException
     * @throws IOException
     */
    public int collectAndUpload() throws ClientProtocolException, IOException {
        HeapSummary hs = getHeapDetails();
        driver.executeUpdate(hs.uploadSQLStatement());
        JSONArray data = driver.executeSelect(hs.getHeapIdSQL());
        int out = data.getJSONObject(0).getInt("heap_id");
        return out;
    }

}
