package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;
import org.json.JSONArray;
import org.json.JSONObject;

import com.salesforce.cpp.heaphub.collect.models.DomTreeObject;
import com.salesforce.cpp.heaphub.collect.models.ThreadInfo;
import com.salesforce.cpp.heaphub.collect.models.ThreadIds;
import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectThreads extends CollectBase {

    String heapName;
    int heapId;
    Long createdAt;
    Long minSize;

    public CollectThreads(String heapName, int heapId, Long createdAt, Long minSize) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.minSize = minSize;
    }

        
    public ArrayList<ThreadInfo> collectThreads(long minSize) {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<ThreadInfo> arr = new ArrayList<ThreadInfo>(32);
            while(loop) {
                ArrayList<ThreadInfo> objs = getThreads(i, 32);
                if (objs == null || objs.size() == 0) {
                    log("____null_____");
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
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<ThreadInfo> getThreads(int page, int pageSize) throws ClientProtocolException, IOException, URISyntaxException {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/threads")
            .addParameter("page", String.valueOf(page))
            .addParameter("pageSize", String.valueOf(pageSize))
            .build();
            HttpGet getThreads = new HttpGet(uri);
            Response res = new Response(CLIENT_SYNC.execute(getThreads));
            if (res.getStatusCode() >= 300) {
                // TODO: throw exception
                return null;
            } else {
                // create json object with res body
                JsonObject resJSON = res.getBody();
                if (resJSON == null) {
                    log("resJSON is null");
                    // TODO: throw exception
                    return null;
                }
                // get data attribute of json body
                JsonArray jsonArray = resJSON.getJsonArray("data");
                if (jsonArray == null) {
                    log("resJSON.data is null");
                    // TODO: throw exception
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

    public void collectAndUpload() throws IOException {
        ArrayList<ThreadInfo> arr = collectThreads(minSize);
        for (ThreadInfo thread : arr) {
            driver.executeUpdate(thread.uploadSQLStatement());
        }
    }

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
