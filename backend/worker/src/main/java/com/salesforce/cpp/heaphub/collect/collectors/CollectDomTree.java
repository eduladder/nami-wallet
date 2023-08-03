package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;

import com.salesforce.cpp.heaphub.collect.models.DomTreeObject;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/***
 * Collector class to collect a heap dump's dominator tree and upload the results to SQL. Since this is a tree data structure, the data collected is restricted by the passed in maximum depth, branching factor and mininimum size of data collected.
 */
public class CollectDomTree extends CollectBase {
    private String heapName; 
    private int heapId;
    private long createdAt;
    private long minSize;
    private int branchingFactor; 
    private int maxDepth;

    /***
     * Constructor
     * @param heapName generated name of the heap
     * @param heapId primary key id of heap in SQL database
     * @param createdAt time when analysis is being conducted
     * @param minSize collect all object with a retained size > minSize
     * @param branchingFactor branching factor to limit collection of children of dominator roots
     * @param maxDepth max depth to limit collection of children of dominator roots
     */
    public CollectDomTree(String heapName, int heapId, long createdAt, long minSize, int branchingFactor, int maxDepth) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.minSize = minSize;
        this.branchingFactor = branchingFactor;
        this.maxDepth = maxDepth;
    }


    /***
     * Collect all dominator roots for a given heap dump
     * @return ArrayList<DomTreeObject> which contains all the dominator roots for a given heap dump
     */
    public ArrayList<DomTreeObject> collectDominatorRoots() {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);
            // collect dominator roots until all are collected for heap dump OR one is collected with retainedSize < minSize
            while(loop) {
                ArrayList<DomTreeObject> objs = getDomTreeRoots(i, 32);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (DomTreeObject obj : objs) {
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

    /**
     * Call JIFA API to get dominator roots for the heap dump. Convert Result to ArrayList<DomTreeObject> 
     * @param page
     * @param pageSize
     * @return ArrayList<DomTreeObject> which contains the dominator roots for a given heap dump for a paged request
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    public ArrayList<DomTreeObject> getDomTreeRoots(int page, int pageSize) throws ClientProtocolException, IOException, URISyntaxException {
        ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);
        // Call Jifa API - result is returned sorted by retained heap size
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/dominatorTree/roots")
        .addParameter("grouping", "NONE")
        .addParameter("page", String.valueOf(page))
        .addParameter("pageSize", String.valueOf(pageSize))
        .build();
        HttpGet getDomTreeRoots = new HttpGet(uri);
        HttpResponse rs = CLIENT_SYNC.execute(getDomTreeRoots);
        Response res = new Response(rs);
        if (res.getStatusCode() >= 300) {
            log("Request Failed");
            return null;
        }
        // Convert result to desired data format
        JsonObject resJSON = res.getBody();
        JsonArray jSONArray = resJSON.getJsonArray("data");
        if (jSONArray == null) {
            log("jsonArray null");
            return null;
        }
        int i = 0;
        for (i = 0; i < jSONArray.size(); i++) {
            JsonObject curr = jSONArray.getJsonObject(i);
            DomTreeObject obj = new DomTreeObject(curr, -1, true, createdAt, heapId);
            arr.add(obj);
        }
        return arr;
    }


    /**
     * Call JIFA API to get dominator children for a given dominator tree root in the heap dump. Convert result to ArrayList<DomTreeObject> 
     * @param page
     * @param pageSize
     * @param parentId
     * @return ArrayList<DomTreeObject> which contains the dominator children for a given heap dump for a paged request
     * @throws ClientProtocolException
      * @throws IOException
      * @throws URISyntaxException
      */
    public ArrayList<DomTreeObject> getDomTreeChildren(int page, int pageSize, int parentId) throws ClientProtocolException, IOException, URISyntaxException {
        // Call Jifa API - result is returned sorted by retained heap size
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/dominatorTree/children")
        .addParameter("grouping", "NONE")
        .addParameter("page", String.valueOf(page))
        .addParameter("pageSize", String.valueOf(pageSize))
        .addParameter("parentObjectId", String.valueOf(parentId))
        .build();
        HttpGet getDomTreeChildrenRequest = new HttpGet(uri); 
        Response res = new Response(CLIENT_SYNC.execute(getDomTreeChildrenRequest));
        if (res.getStatusCode() >= 300) {
            // TODO: throw exception
            return null;
        } else {
            // Convert result to desired data format
            JsonObject resJSON = res.getBody();
            if (resJSON == null) {
                log("resJSON is null");
                // TODO: throw exception
                return null;
            }
            JsonArray jSONArray = resJSON.getJsonArray("data");
            if (jSONArray == null) {
                log("resJSON.data is null");
                return null;
            }
            ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);
            for (int i = 0; i < jSONArray.size(); i++) {
                JsonObject curr = jSONArray.getJsonObject(i);
                DomTreeObject obj = new DomTreeObject(curr, parentId, false, createdAt, heapId);
                arr.add(obj);
            }
            return arr;
        }
    }

    /***
     * Collect all dominator children for a given domintator root
     * @param parentId - the id of the root to look for the children of 
     * @return ArrayList<DomTreeObject> which contains all the dominator roots for a given heap dump
     * @throws IOException
     */
    private ArrayList<DomTreeObject> collectDominatorChildren(int parentId) throws IOException {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<DomTreeObject> output = new ArrayList<DomTreeObject>(128);
            int cnt = 0;
            // Collect children until there are no more children or the number of children collected surpasses the branching factor
            while(loop) {
                ArrayList<DomTreeObject> objs = getDomTreeChildren(i, 32, parentId);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (DomTreeObject obj : objs) {
                    output.add(obj);
                    cnt++;
                    if (cnt >= branchingFactor) {
                        loop = false;
                        break;
                    }
                }
                i++;
                if (objs.size() < 32) {
                    break;
                }
            }
            return output;
        } catch (Exception e){
            log(e);
            return null;
        }
    }

    public  ArrayList<DomTreeObject> addChildren( ArrayList<DomTreeObject> roots) throws IOException {
        // Stack class to keep track of the stack entry and the depth of the entry
        class StackObjInfo {
            public DomTreeObject obj;
            public int depth;
            public StackObjInfo(DomTreeObject obj, int depth) {
                this.obj = obj;
                this.depth = depth;
            }
        }
        // Conduct a DFS traversal on the dominator tree to collect all the children
        Stack<StackObjInfo> stack = new Stack<StackObjInfo>();
        // init stack by adding all roots
        for (DomTreeObject root : roots) {
            stack.push(new StackObjInfo(root, 0));
        }
        
        ArrayList<DomTreeObject> output = new ArrayList<DomTreeObject>();        
        // conduct DFS traversal
        while (!stack.isEmpty()) {
            StackObjInfo curr = stack.pop();
            output.add(curr.obj);
            // do not expand current object if maxDepth has been reached
            if (curr.depth < maxDepth) {
                for (DomTreeObject obj : collectDominatorChildren(curr.obj.getObjectId())) {
                    // a child is added to the stack (and hence has its children expanded) if the retained size is greater than the min size
                    // otherwise the child is simply added to the output list so that it is still collected, but its children are not
                    if (obj.getRetainedSize() >= minSize) {
                        stack.push(new StackObjInfo(obj, curr.depth + 1));
                    } else {
                        output.add(obj);
                    }
                }
            }
        }
        return output;
    }

    /***
     * Collect all the dominator roots and their children, upload the results to SQL
     * @return ArrayList<DomTreeObject> which contains all the dominator roots and their children
     * @throws IOException
     */
    public ArrayList<DomTreeObject> collectAndUpload() throws IOException {
        ArrayList<DomTreeObject> domRoots = collectDominatorRoots();
        ArrayList<DomTreeObject> domTree = addChildren (domRoots);
        StringBuilder sb = new StringBuilder(DomTreeObject.uploadSQLStatement());
        int cnt = 0;
         // use batch insert with batch size of 100 to upload data
        for (DomTreeObject obj : domTree) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(obj.getSQLValues());
            cnt++;
            if (cnt == 100) {
                sb.append(";");
                driver.executeUpdate(sb.toString());
                sb = new StringBuilder(DomTreeObject.uploadSQLStatement());
                cnt = 0;
            }
        }
        if (cnt != 0) {
            sb.append(";");
            driver.executeUpdate(sb.toString());
        }
        return domRoots;
    }

}
