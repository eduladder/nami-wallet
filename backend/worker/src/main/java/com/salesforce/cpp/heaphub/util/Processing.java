package com.salesforce.cpp.heaphub.util;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jifa.worker.Constant;

import io.vertx.core.json.JsonObject;

public class Processing {
    static CloseableHttpClient CLIENT_SYNC = HttpClients.createDefault();
    
    // public static class HeapDumpOverview {
    //   String originalName;
    //   String generatedName;
    //   Long heapCreationDate;
      
    //   public HeapDumpOverview(String originalName, String generatedName, long heapCreationDate) {
    //     this.originalName = originalName;
    //     this.generatedName = generatedName;
    //     this.heapCreationDate = heapCreationDate;
    //   }

    //   public HeapDumpOverview (JsonObject obj) {
    //     this.originalName = obj.getString("originalName");
    //     this.generatedName = obj.getString("name");
    //     this.heapCreationDate = obj.getLong("createdAt");
    //   }
    // }
    public static void Analyze(String fileName) throws ClientProtocolException, IOException {
      HttpPost analyze = new HttpPost(Constant.API.HEAP_DUMP_API_PREFIX + "/" + fileName + "/analyze");
      CLIENT_SYNC.execute(analyze);
		// Response res = new Response(CLIENT_SYNC.execute(analyze));
    //         if (res.getStatusCode() >= 300) {
    //         return null;
    //     }
    //     JsonObject resJSON = res.getBody();
    //     if (resJSON == null) {
    //         return null;
    //     }
    //     return new HeapDumpOverview(resJSON);
    }
}
