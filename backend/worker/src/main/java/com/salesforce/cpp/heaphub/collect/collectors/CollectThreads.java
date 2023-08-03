package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;
import org.json.JSONArray;
import org.json.JSONObject;

import com.salesforce.cpp.heaphub.collect.models.ThreadInfo;
import com.salesforce.cpp.heaphub.collect.models.ThreadIds;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Collector class to collect a heap dump's thread. Upload the results to SQL. Only collect threads with size > minSize
 */
public class CollectThreads extends CollectBase {

    String heapName;
    int heapId;
    Long createdAt;
    Long minSize;


    /***
     * Constructor for CollectThreads
     * @param heapName generated name for the heap 
      * @param heapId primary key id of heap in SQL database
     * @param createdAt time when analysis is being conducted
      * @param minSize minimum retained size of the threads to collect
     */
    public CollectThreads(String heapName, int heapId, Long createdAt, Long minSize) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.minSize = minSize;
    }
    
    
    /***
     * Collects all the threads with size > minSize of heap dump
     * @return ArrayList<ThreadInfo>
     * @throws IOException
     */
    public ArrayList<ThreadInfo> collectThreads(long minSize) throws IOException {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<ThreadInfo> arr = new ArrayList<ThreadInfo>(32);
            // collect threads until retained size < minSize
            while(loop) {
                ArrayList<ThreadInfo> objs = getThreads(i, 32);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (ThreadInfo obj : objs) {
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

    /**
     * paged call to JIFA API to get the threads
     * @param page
     * @param pageSize
     */
    public ArrayList<ThreadInfo> getThreads(int page, int pageSize) throws ClientProtocolException, IOException, URISyntaxException {
        // call JIFA API - assume result is sorted by thread retained size
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/threads")
        .addParameter("page", String.valueOf(page))
        .addParameter("pageSize", String.valueOf(pageSize))
        .build();
        HttpGet getThreads = new HttpGet(uri);
        Response res = new Response(CLIENT_SYNC.execute(getThreads));
        if (res.getStatusCode() >= 300) {
            return null;
        } else {
            // parse resonse and convert to data type
            JsonObject resJSON = res.getBody();
            if (resJSON == null) {
                log("resJSON is null");
                return null;
            }
            JsonArray jsonArray = resJSON.getJsonArray("data");
            if (jsonArray == null) {
                log("resJSON.data is null");
                return null;
            }
            ArrayList<ThreadInfo> arr = new ArrayList<ThreadInfo>(32);
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject curr = jsonArray.getJsonObject(i);
                ThreadInfo thread = new ThreadInfo(curr, heapId, createdAt);
                arr.add(thread);
            }
            return arr;
        }
    }

    /***
     * Collect the threads and upload to SQL
     * @throws IOException
     */
    public void collectAndUpload() throws IOException {
        ArrayList<ThreadInfo> arr = collectThreads(minSize);
        StringBuilder sb = new StringBuilder(ThreadInfo.uploadSQLStatement());
        int cnt = 0;
        for (ThreadInfo obj : arr) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(obj.getSQLValues());
            cnt++;
            if (cnt == 100) {
                sb.append(";");
                driver.executeUpdate(sb.toString());
                sb = new StringBuilder(ThreadInfo.uploadSQLStatement());
                cnt = 0;
            }
        }
        if (cnt != 0) {
            sb.append(";");
            driver.executeUpdate(sb.toString());
        }
    }

    /**
     * Get the thread ids from the SQL database (the object id of each thread and the sql generated primary key for the thread) - used to collect stack trace of heap 
     * @return
     * @throws IOException
     */
    public ArrayList<ThreadIds> getThreadIds() throws IOException {
        JSONArray data = driver.executeSelect(ThreadInfo.getIds(heapId));
        ArrayList<ThreadIds> threadIds = new ArrayList<ThreadIds>();
        for (int i = 0; i < data.length(); i++) {
            JSONObject obj = data.getJSONObject(i);
            threadIds.add(new ThreadIds(obj.getInt("thread_info_id"), obj.getInt("object_id")));
        }
        return threadIds;
    }
}
