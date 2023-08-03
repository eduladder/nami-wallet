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
    /** 
     * Analyze an inputted heap dump. All metrics for the dump are collected and uploaded to SQL database. This is connected to the "/file/transferByURL" route in FileRoute.java
     * @param originalName
     * @param generatedName
     * @param heapCreationDate - creation date of heap dump in milliseconds
\     * @throws IOException
     * @throws ParseException
      * @throws URISyntaxException
     */
    public static void collect(String originalName, String generatedName, long heapCreationDate) throws IOException, ParseException, URISyntaxException {
        Processing.Analyze(generatedName);
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
    }
}
