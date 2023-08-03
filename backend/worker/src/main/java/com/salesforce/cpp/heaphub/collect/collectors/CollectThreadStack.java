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
import com.salesforce.cpp.heaphub.collect.models.ThreadStackLocal;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


/**
 * Collector class to collect a heap dump's stack trace and locals for all collected threads. Upload to SQL
 */
public class CollectThreadStack extends CollectBase {
    private int heapId;
    private long createdAt;
    private String heapName;
    private ArrayList<ThreadIds> idPairs;
    

    /***
     * Constructor for CollectThreadStack
     * @param heapName generated name for the heap 
     * @param heapId primary key id of heap in SQL database
     * @param createdAt time when analysis is being conducted
     * @param idPairs list of pairs of thread ids and their sql primary key ids
     */
    public CollectThreadStack(String heapName, int heapId, long createdAt, ArrayList<ThreadIds> idPairs) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.idPairs = idPairs;
    }
    

    /***
     * Collects all the stack traces for all the threads in the heap dump
     * @return ArrayList<ThreadStackLocal>
     * @throws IOException
     * @throws URISyntaxException
     * @throws ClientProtocolException
     * @throws ParseException
     */
    public ArrayList<ThreadStackLocal> collectStackTraces() throws IOException, URISyntaxException {
        ArrayList<ThreadStackLocal> acc = new ArrayList<ThreadStackLocal>();
        for (ThreadIds thread : idPairs) {
            ArrayList<ThreadStackLocal> stacktrace = getStackTrace(thread);
            acc.addAll(stacktrace);
        }
        return acc;
    }


    /***
     * Collects the stack trace for a specific thread
     * @param threadId id of the thread
      * @return ArrayList<ThreadStackLocal>
     * @throws IOException
     * @throws URISyntaxException
      * @throws ClientProtocolException
     * @throws ParseException
     */
    public ArrayList<ThreadStackLocal> getStackTrace(ThreadIds threadId) throws ClientProtocolException, IOException, URISyntaxException {
        // call JIFA API
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/stackTrace")
		.addParameter("objectId", String.valueOf(threadId.thread_id))
		.build();
		HttpGet getStackTrace = new HttpGet(uri);
        Response res = new Response(CLIENT_SYNC.execute(getStackTrace));
        if (res.getStatusCode() >= 300) {
            log("req failure");
            return null;
        } else {
            // parse response
            if (res.getBody() == null) {
                log("body is null");
                return null;
            }
            JsonArray jsonArray = res.getBody().getJsonArray("trace");
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


    /***
     * Collects the locals for a specific stack trace element
     * @param stack stack trace element to collect locals for
      * @return ArrayList<ThreadStackLocal>
     * @throws IOException
     * @throws URISyntaxException
      * @throws ClientProtocolException
     * @throws ParseException
     */
    public ArrayList<ThreadStackLocal> getLocals(ThreadStackLocal stack) throws URISyntaxException, ParseException, ClientProtocolException, IOException {
        // call the api
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/locals")
		.addParameter("objectId", String.valueOf(stack.getThreadId()))
        .addParameter("firstNonNativeFrame", String.valueOf(stack.isFirstNonNativeFrame()))
        .addParameter("depth", String.valueOf(stack.getDepth()))
		.build();
		HttpGet getLocals = new HttpGet(uri);
        Response res = new Response(CLIENT_SYNC.execute(getLocals));
        if (res.getStatusCode() >= 300) {
            log(stack);
            log("req failure");
            return null;
        } else {
            // parse the response and convert to proper data format
            if (res.getBody() == null) {
                log("body is null");
                return null;
            }
            JsonArray jsonArray = res.getBody().getJsonArray("localVariables");
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


    /***
     * collect locals for the stack trace elements
     * @param stacks stack trace elements to collect locals for
     * @return ArrayList<ThreadStackLocal>
     * @throws IOException
     * @throws URISyntaxException
     * @throws ParseException
     */
    public ArrayList<ThreadStackLocal> addLocals(ArrayList<ThreadStackLocal> stacks) throws ParseException, ClientProtocolException, URISyntaxException, IOException {
        ArrayList<ThreadStackLocal> collecter = new ArrayList<ThreadStackLocal>();
        for (ThreadStackLocal stack : stacks) {
            if (stack.hasLocal()) {
                collecter.addAll(getLocals(stack));
            } else {
                collecter.add(stack);
            }
        }
        return collecter;    
    }

    /***
     * Collects the stack trace and locals for each thread of the heap dump
     * @return ArrayList<ThreadStackLocal>
     * @throws IOException
     * @throws URISyntaxException
     * @throws ParseException
    */
    public ArrayList<ThreadStackLocal> collectTraceAndLocals() throws ParseException, ClientProtocolException, URISyntaxException, IOException {
        ArrayList<ThreadStackLocal> stacks = collectStackTraces();
        return addLocals(stacks);        
    }

     /***
     * main entry to collect the trace and locals for each thread of the heap dump and upload to database
     */
    public void collectAndUpload() throws ParseException, ClientProtocolException, URISyntaxException, IOException {
        ArrayList<ThreadStackLocal> stacksAndLocals = collectTraceAndLocals();
        StringBuilder sb = new StringBuilder(ThreadStackLocal.uploadSQLStatement());
        int cnt = 0;
        // use batch insert with batch size of 100 to upload data
        for (ThreadStackLocal tsl : stacksAndLocals) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(tsl.getSQLValues());
            cnt++;
            // set batch size
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
