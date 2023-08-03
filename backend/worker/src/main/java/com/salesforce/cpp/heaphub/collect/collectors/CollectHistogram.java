package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;

import com.salesforce.cpp.heaphub.collect.models.ClassHistoInfo;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class CollectHistogram extends CollectBase {
    String heapName;
    int heapId;
    long createdAt;
    long minSize;

    /***
     * Constructor for CollectHistogram
     * @param heapName generated name for the heap 
     * @param heapId primary key id of heap in SQL database
     * @param createdAt time when analysis is being conducted
     * @param minSize minimum size of the histogram classes to collect
     */
    public CollectHistogram(String heapName, int heapId, long createdAt, long minSize) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.minSize = minSize;
    }


    /***
     * Collects all the histogram classes
     * @return ArrayList<ClassHistoInfo> which contains all the histogram classes and their sizes
     * @throws IOException
     */
    public ArrayList<ClassHistoInfo> collectHistogram() throws IOException {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<ClassHistoInfo> arr = new ArrayList<ClassHistoInfo>(32);
            // collect histogram classes until retained size of returned class < minSize
            while(loop) {
                ArrayList<ClassHistoInfo> objs = getHistogram(i, 32);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (ClassHistoInfo obj : objs) {
                    if (obj.getRetainedSize() < minSize) {
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
            log(e);
            return null;
        }
    }


    /***
     * Get the histogram roots for a given page and page size
     * @param page
      * @param pageSize
     * @return ArrayList<ClassHistoInfo> which contains all the histogram classes and their sizes
     * @throws IOException
     */
    public ArrayList<ClassHistoInfo> getHistogram(int page, int pageSize) throws IOException {
        try {
            // make request to JIFA API - assumes rsult sorted by retained size
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
                // convert to ClassHistoInfo
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
            log(e);
            return null;
        }
    }
    
    /***
     * Collect the histogram and upload to SQL
     * @return ArrayList<ClassHistoInfo> which contains all the histogram classes and their sizes
     * @throws IOException
     */
    public ArrayList<ClassHistoInfo> collectAndUpload() throws IOException {
        ArrayList<ClassHistoInfo> arr = collectHistogram();
        StringBuilder sb = new StringBuilder(ClassHistoInfo.uploadSQLStatement());
        int cnt = 0;
        // use batch insert with batch size of 100 to upload data
        for (ClassHistoInfo obj : arr) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(obj.getSQLValues());
            cnt++;
            // set batch size
            if (cnt == 100) {
                sb.append(";");
                driver.executeUpdate(sb.toString());
                sb = new StringBuilder(ClassHistoInfo.uploadSQLStatement());
                cnt = 0;
            }
        }
        if (cnt != 0) {
            sb.append(";");
            driver.executeUpdate(sb.toString());
        }
        return arr;
    }
    
}
