package com.salesforce.cpp.heaphub.route;

import java.util.ArrayList;

import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;

import com.salesforce.cpp.heaphub.collect.collectors.CollectClassReference;
import com.salesforce.cpp.heaphub.collect.collectors.CollectDomTree;
import com.salesforce.cpp.heaphub.collect.collectors.CollectHeapSummary;
import com.salesforce.cpp.heaphub.collect.collectors.CollectHistogram;
import com.salesforce.cpp.heaphub.collect.collectors.CollectThreadStack;
import com.salesforce.cpp.heaphub.collect.collectors.CollectThreads;
import com.salesforce.cpp.heaphub.collect.models.ClassHistoInfo;
import com.salesforce.cpp.heaphub.collect.models.DomTreeObject;
import com.salesforce.cpp.heaphub.collect.models.ThreadIds;


import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A sample route to collect all information for a heapdump. Main purpose is for testing without using UI
 */
public class HeapHubCollectRoutes extends HeapHubBaseRoute {
    @RouteMeta(path = "/collect", method = HttpMethod.GET)
    void collect(Future<JsonObject> future, RoutingContext context, @ParamKey("file") String file, @ParamKey("dominatorMinSize") long dominatorMinSize, @ParamKey("branchingFactor") int branchingFactor, @ParamKey("maxDepth") int maxDepth, @ParamKey("maxOutbounds") int maxOutbounds, @ParamKey("histoMinSize") long histoMinSize, @ParamKey("threadMinSize") long threadMinSize) throws Exception{
        String originalName = file.split("-", 2)[1];
        long heapCreationDate = Long.parseLong(originalName.split("-", 2)[0]);
        String generatedName = file;
        logFile.delete();
        logFile.createNewFile();
        long currTime = System.currentTimeMillis();
        CollectHeapSummary hs = new CollectHeapSummary(originalName, generatedName,heapCreationDate, currTime);
        int heapId = hs.collectAndUpload();
        CollectDomTree cdt = new CollectDomTree(generatedName, heapId, currTime, 50*1000*1000, 10, 2);
        // roots are used in collection of outbounds and GCRoot path, but this is not currently included because SQL DB is not set up yet
        ArrayList<DomTreeObject> roots = cdt.collectAndUpload();
        CollectHistogram ch = new CollectHistogram(generatedName, heapId, currTime, 150*1000*1000);
        ArrayList<ClassHistoInfo> histogram = ch.collectAndUpload();
        CollectThreads ct = new CollectThreads(generatedName, heapId, currTime, (long) 10*1000*1000);
        ct.collectAndUpload();
        ArrayList<ThreadIds> threadIds = ct.getThreadIds();
        CollectThreadStack cts = new CollectThreadStack(generatedName, heapId, currTime, threadIds);
        cts.collectAndUpload();
        CollectClassReference ccr = new CollectClassReference(heapId, generatedName, currTime, histogram, 2, 20);
        ccr.collectAndUpload();
        future.complete(new JsonObject("{\"success\": \"true\"}"));
    }

}
