package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.eclipse.jifa.worker.Constant;
import org.json.JSONArray;

import com.salesforce.cpp.heaphub.collect.models.HeapSummary;
import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectHeapSummary extends CollectBase {

    String generatedName;
    String originalName;
    long createdAt;
    long heapCreationDate;

    public CollectHeapSummary (String originalName, String generatedName, long heapCreationDate, long createdAt) {
        this.originalName = originalName;
        this.generatedName = generatedName;
        this.heapCreationDate = heapCreationDate;
        this.createdAt = createdAt;
    }
    
    public HeapSummary getHeapDetails() throws ClientProtocolException, IOException {
        Response res = new Response(CLIENT_SYNC.execute(heapDetailsRequest()));
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

    HttpUriRequest heapDetailsRequest() {
		HttpGet getSummary = new HttpGet(Constant.API.HEAP_DUMP_API_PREFIX + "/" + generatedName + "/details"); 
		return getSummary;
    }

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

    public int collect() throws ClientProtocolException, IOException {
        HeapSummary hs = getHeapDetails();
        driver.executeUpdate(hs.uploadSQLStatement());
        JSONArray data = driver.executeSelect(hs.getHeapIdSQL());
        int out = data.getJSONObject(0).getInt("heap_id");
        return out;
    }

}
