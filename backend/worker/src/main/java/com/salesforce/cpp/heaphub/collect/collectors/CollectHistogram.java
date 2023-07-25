package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;

import com.salesforce.cpp.heaphub.collect.models.ClassHistoInfo;
import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;
import com.salesforce.cpp.heaphub.util.Response;
import com.salesforce.cpp.heaphub.common.Common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import com.opencsv.CSVWriter;


public class CollectHistogram extends CollectBase {
    String heapName;
    int heapId;
    long createdAt;
    long minSize;

    public CollectHistogram(String heapName, int heapId, long createdAt, long minSize) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.minSize = minSize;
    }

    public ArrayList<ClassHistoInfo> collectHistogram(long minSize) {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<ClassHistoInfo> arr = new ArrayList<ClassHistoInfo>(32);
            while(loop) {
                ArrayList<ClassHistoInfo> objs = getHistogramRoots(i, 32);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (ClassHistoInfo obj : objs) {
                    if (obj.getShallowSize() < minSize) {
                        loop = false;
                        break;
                    } else {
                        arr.add(obj);
                    }
                }
                i++;
            }
            return arr;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<ClassHistoInfo> getHistogramRoots(int page, int pageSize) {
        try {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/histogram")
            .addParameter("groupingBy", "BY_CLASS")
            .addParameter("page", String.valueOf(page))
            .addParameter("pageSize", String.valueOf(pageSize))
            .build();
            HttpGet getHistoRoots = new HttpGet(uri); 
            Response res = new Response(CLIENT_SYNC.execute(getHistoRoots));
            if (res.getStatusCode() >= 300) {
                // TODO
                return null;
            } else {
                JsonObject resJSON = res.getBody();
                if (resJSON == null) {
                    // TODO
                    return null;
                }

                JsonArray jsonArray = resJSON.getJsonArray("data");
                if (jsonArray == null) {
                    log("resJSON.data is null");
                    // TODO: throw exception
                    return null;
                }
                ArrayList<ClassHistoInfo> arr = new ArrayList<ClassHistoInfo>(32);

                for(int i = 0; i < jsonArray.size(); i++) {
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new ClassHistoInfo(curr, heapId, createdAt));
                }
                return arr;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }
    
    public void collectAndUpload() throws IOException {
        ArrayList<ClassHistoInfo> arr = collectHistogram(minSize);
        for (ClassHistoInfo chi : arr) {
                driver.executeUpdate(chi.uploadSQLStatement());
        }
    }


    public void collectCSVAndUpload() throws IOException {
        File file = new File(Common.csvDestination + "/histogram.csv");
        FileWriter outputfile = new FileWriter(file);
        CSVWriter writer = new CSVWriter(outputfile);
        ArrayList<ClassHistoInfo> arr = collectHistogram(minSize);
        writer.writeNext(ClassHistoInfo.getCSVHeader());
        for (ClassHistoInfo chi : arr) {
            writer.writeNext(chi.getCSVArray());
        }
        driver.executeUpdate(ClassHistoInfo.uploadCSV(file.getAbsolutePath()));
        file.delete();
    }
    
}
