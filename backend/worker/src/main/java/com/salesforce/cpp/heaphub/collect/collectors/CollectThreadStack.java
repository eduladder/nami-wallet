package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;

import com.salesforce.cpp.heaphub.collect.models.ThreadIds;
import com.salesforce.cpp.heaphub.collect.models.ThreadInfo;
import com.salesforce.cpp.heaphub.collect.models.ThreadStackLocal;
import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectThreadStack extends CollectBase {
    private int heapId;
    private long createdAt;
    private String heapName;
    private ArrayList<ThreadIds> idPairs;
    
    public CollectThreadStack(String heapName, int heapId, long createdAt, ArrayList<ThreadIds> idPairs) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.idPairs = idPairs;
    }
    
    public ArrayList<ThreadStackLocal> collectStackTraces() throws IOException, URISyntaxException {
        // try {
            ArrayList<ThreadStackLocal> acc = new ArrayList<ThreadStackLocal>();
            for (ThreadIds thread : idPairs) {
                ArrayList<ThreadStackLocal> stacktrace = getStackTrace(thread);
                acc.addAll(stacktrace);
            }
            return acc;
        // } catch (Exception e) {
        //     log("collectStackTrace null pointer");
        //     log(e);
        //     log(e.getMessage());
        //     return null;
        // }
    }

    public ArrayList<ThreadStackLocal> getStackTrace(ThreadIds threadId) throws ClientProtocolException, IOException, URISyntaxException {
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/stackTrace")
		.addParameter("objectId", String.valueOf(threadId.thread_id))
		.build();
		HttpGet getStackTrace = new HttpGet(uri);
        Response res = new Response(CLIENT_SYNC.execute(getStackTrace));
        if (res.getStatusCode() >= 300) {
            // TODO: throw exception
            log("req failure");
            return null;
        } else {
            // create json object with res body
            if (res.getBody() == null) {
                log("body is null");
                // TODO: throw exception
                return null;
            }
            JsonArray jsonArray = res.getBody().getJsonArray("trace");
            // TODO: update with proper return format
            ArrayList<ThreadStackLocal> arr = new ArrayList<ThreadStackLocal>(32);
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject curr = jsonArray.getJsonObject(i);
                ThreadStackLocal stack = new ThreadStackLocal(heapId, createdAt, threadId.thread_id, threadId.thread_info_id, i+1);
                stack.addStackInfo(curr);
                arr.add(stack);
            }
            return arr;
        }
    }

    public ArrayList<ThreadStackLocal> getLocals(ThreadStackLocal stack) throws URISyntaxException, ParseException, ClientProtocolException, IOException {
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/locals")
		.addParameter("objectId", String.valueOf(stack.getThreadId()))
        .addParameter("firstNonNativeFrame", String.valueOf(stack.isFirstNonNativeFrame()))
        .addParameter("depth", String.valueOf(stack.getDepth()))
		.build();
		HttpGet getLocals = new HttpGet(uri);
        Response res = new Response(CLIENT_SYNC.execute(getLocals));
        if (res.getStatusCode() >= 300) {
            // TODO: throw exception
            log(stack);
            log("req failure");
            return null;
        } else {
            // create json object with res body
            if (res.getBody() == null) {
                log("body is null");
                // TODO: throw exception
                return null;
            }
            JsonArray jsonArray = res.getBody().getJsonArray("localVariables");
            // TODO: update with proper return format
            ArrayList<ThreadStackLocal> arr = new ArrayList<ThreadStackLocal>(32);
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject curr = jsonArray.getJsonObject(i);
                ThreadStackLocal currLocal = stack.copy();
                currLocal.addLocalsInfo(curr);
                arr.add(currLocal);
            }
            if (jsonArray.size() == 0) {
                arr.add(stack);
            }
            return arr;
        }
    }

    public ArrayList<ThreadStackLocal> addLocals(ArrayList<ThreadStackLocal> stacks) throws ParseException, ClientProtocolException, URISyntaxException, IOException {
        log(stacks.size());
        ArrayList<ThreadStackLocal> collecter = new ArrayList<ThreadStackLocal>();
        for (ThreadStackLocal stack : stacks) {
            if (stack.hasLocal()) {
                collecter.addAll(getLocals(stack));
            } else {
                collecter.add(stack);
            }
        }
        log(collecter.size());
        return collecter;    
    }

    public ArrayList<ThreadStackLocal> collectTraceAndLocals() throws ParseException, ClientProtocolException, URISyntaxException, IOException {
        ArrayList<ThreadStackLocal> stacks = collectStackTraces();
        return addLocals(stacks);        
    }

    public void collectAndUpload() throws ParseException, ClientProtocolException, URISyntaxException, IOException {
        ArrayList<ThreadStackLocal> stacksAndLocals = collectTraceAndLocals();
        StringBuilder sb = new StringBuilder(ThreadStackLocal.uploadSQLStatement());
        int cnt = 0;
        for (ThreadStackLocal tsl : stacksAndLocals) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(tsl.getSQLValues());
            cnt++;
            if (cnt == 100) {
                sb.append(";");
                driver.executeUpdate(sb.toString());
                sb = new StringBuilder(ThreadStackLocal.uploadSQLStatement());
                cnt = 0;
            }
        }
        if (cnt != 0) {
            sb.append(";");
            driver.executeUpdate(sb.toString());
        }
    }
}
