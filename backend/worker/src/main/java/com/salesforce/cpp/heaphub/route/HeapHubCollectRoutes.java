package com.salesforce.cpp.heaphub.route;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.vo.heapdump.dominatortree.BaseRecord;

import com.salesforce.cpp.heaphub.collect.Collect;
import com.salesforce.cpp.heaphub.collect.collectors.CollectHeapSummary;
import com.salesforce.cpp.heaphub.collect.models.HeapSummary;
import com.salesforce.cpp.heaphub.util.Response;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class HeapHubCollectRoutes extends HeapHubBaseRoute{
    // Test Route: 
    // http://localhost:8102/jifa-api/heaphub/2_eu35.hprof/collect?dominatorMinSize=50000000&maxDepth=2&branchingFactor=10&maxOutbounds=10&histoMinSize=10000000&threadMinSize=10000000

    @RouteMeta(path = "/collect", method = HttpMethod.GET)
    void collect(Future<JsonObject> future, RoutingContext context, @ParamKey("file") String file, @ParamKey("dominatorMinSize") long dominatorMinSize, @ParamKey("branchingFactor") int branchingFactor, @ParamKey("maxDepth") int maxDepth, @ParamKey("maxOutbounds") int maxOutbounds, @ParamKey("histoMinSize") long histoMinSize, @ParamKey("threadMinSize") long threadMinSize) throws Exception{
        logFile.delete();
        logFile.createNewFile();
        // String dest = "/Users/dbarra/git/heaphub/outputs";
        // Collect collect = new Collect(file);
        // collect.collectAsCSV(dest, dominatorMinSize, branchingFactor, maxDepth, maxOutbounds, histoMinSize, threadMinSize);
        long currTime = System.currentTimeMillis();
        CollectHeapSummary hs = new CollectHeapSummary(file, 1, currTime);
        hs.getHeapId();
        // hs.collect();
        
        future.complete(new JsonObject("{\"success\": \"true\"}"));
    }

}
