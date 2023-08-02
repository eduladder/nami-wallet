package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.ParseException;

import com.salesforce.cpp.heaphub.collect.models.ClassHistoInfo;
import com.salesforce.cpp.heaphub.collect.models.DomTreeObject;
import com.salesforce.cpp.heaphub.collect.models.ThreadIds;
import com.salesforce.cpp.heaphub.util.Processing;

public class CollectAll extends CollectBase {

    public static void collect(String originalName, String generatedName, long heapCreationDate) throws IOException, ParseException, URISyntaxException {
        Processing.Analyze(generatedName);
        long currTime = System.currentTimeMillis();
        CollectHeapSummary hs = new CollectHeapSummary(originalName, generatedName,heapCreationDate, currTime);
        int heapId = hs.collect();
        CollectDomTree cdt = new CollectDomTree(generatedName, heapId, currTime, 50*1000*1000, 10, 2);
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
    }
}
