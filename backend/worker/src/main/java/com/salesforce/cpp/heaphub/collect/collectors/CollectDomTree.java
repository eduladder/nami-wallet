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
import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectDomTree extends CollectBase {
    String heapName;
    int heapId;
    long createdAt;
    long minSize;
    int branchingFactor;
    int maxDepth;

    public CollectDomTree(String heapName, int heapId, long createdAt) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
    }

    public ArrayList<DomTreeObject> collectDominatorRoots(long minSize) {
    try {
        int i = 1;
        boolean loop = true;
        ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);
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
        // log("Collect Dominator Roots: " + arr);
        return arr;
    } catch (Exception e){
        e.printStackTrace();
        return null;
    }
}

public ArrayList<DomTreeObject> getDomTreeRoots(int page, int pageSize) throws ClientProtocolException, IOException, URISyntaxException {
    ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);

    URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/dominatorTree/roots")
    .addParameter("grouping", "NONE")
    .addParameter("page", String.valueOf(page))
    .addParameter("pageSize", String.valueOf(pageSize))
    .build();
    HttpGet getDomTreeRoots = new HttpGet(uri);
    // Cannot use Vert-X because need synchronouse client
    HttpResponse rs = CLIENT_SYNC.execute(getDomTreeRoots);
    Response res = new Response(rs);
    if (res.getStatusCode() >= 300) {
        log("Request Failed");
        return null;
    }
    JsonObject resJSON = res.getBody();
    JsonArray jSONArray = resJSON.getJsonArray("data");
    if (jSONArray == null) {
        log("jsonArray null");
        return null;
    }
    int i = 0;
    // get first index in array

    for (i = 0; i < jSONArray.size(); i++) {
        JsonObject curr = jSONArray.getJsonObject(i);
        DomTreeObject obj = new DomTreeObject(curr, -1, true, createdAt, heapId);
        arr.add(obj);
    }
    // log("getDomTree Return: " + arr);
    return arr;
}

public ArrayList<DomTreeObject> getDomTreeChildren(int page, int pageSize, int parentId) throws ClientProtocolException, IOException, URISyntaxException {
        // call api
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
            // create json object with res body
            JsonObject resJSON = res.getBody();
            if (resJSON == null) {
                log("resJSON is null");
                // TODO: throw exception
                return null;
            }
            // get data attribute of json body
            JsonArray jSONArray = resJSON.getJsonArray("data");
            if (jSONArray == null) {
                log("resJSON.data is null");
                // TODO: throw exception
                return null;
            }
            ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);
            // get first index in array
            for (int i = 0; i < jSONArray.size(); i++) {
                JsonObject curr = jSONArray.getJsonObject(i);
                DomTreeObject obj = new DomTreeObject(curr, parentId, false, createdAt, heapId);
                arr.add(obj);
            }
            return arr;
        }
    }

private ArrayList<DomTreeObject> collectDominatorChildren(int parentId, int branchingFactor) {
    try {
        int i = 1;
        boolean loop = true;
        ArrayList<DomTreeObject> output = new ArrayList<DomTreeObject>(128);
        int cnt = 0;
        while(loop) {
            ArrayList<DomTreeObject> objs = getDomTreeChildren(i, 32, parentId);
            if (objs == null || objs.size() == 0) {
                break;
            }
            for (DomTreeObject obj : objs) {
            // if (dominatorTree.containsKey(obj.getLabel())) {
            //     writeError("DUPLICATE KEY: " + obj.getLabel());
            // }
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
        e.printStackTrace();
        return null;
    }
}

public  ArrayList<DomTreeObject> addChildren( ArrayList<DomTreeObject> roots, long minSize, int branchingFactor, int maxDepth) {
    class StackObjInfo {
        public DomTreeObject obj;
        public int depth;
        public StackObjInfo(DomTreeObject obj, int depth) {
            this.obj = obj;
            this.depth = depth;
        }
    }
    
    Stack<StackObjInfo> stack = new Stack<StackObjInfo>();
    // HashMap<Integer, Integer> parent = new HashMap<Integer, Integer>();
    for (DomTreeObject root : roots) {
        stack.push(new StackObjInfo(root, 0));
    }
    // parent hashmap
    
    ArrayList<DomTreeObject> output = new ArrayList<DomTreeObject>();        
    
    while (!stack.isEmpty()) {
        StackObjInfo curr = stack.pop();
        output.add(curr.obj);
        // int id = curr.getObjectId();
        // while (id != -1) {
        //     path.add(id);
        //     // id = parent.get(id);
        // }
        // int[] pathArr = path.stream().filter(i -> i != null).mapToInt(i -> i).toArray();
        if (curr.depth < maxDepth) {
            for (DomTreeObject obj : collectDominatorChildren(curr.obj.getObjectId(), branchingFactor)) {
                if (obj.getRetainedSize() >= minSize) {
                    stack.push(new StackObjInfo(obj, curr.depth + 1));
                    // parent.put(obj.getObjectId(), curr.getObjectId());
                } else {
                    // if less than minSize, add to collected objects but do not further expand
                    output.add(obj);
                }
            }
        }
    }
    return output;
}

    public ArrayList<DomTreeObject> collect() {
        ArrayList<DomTreeObject> domRoots = collectDominatorRoots(minSize);
        ArrayList<DomTreeObject> domTree = addChildren (domRoots, minSize, branchingFactor, maxDepth);
        return domTree;
    }

    public void uploadToSQL() throws IOException {
       ArrayList<DomTreeObject> domTree = collect();
       for (DomTreeObject obj : domTree) {
            HeapHubDatabaseManager.getInstance().executeUpdate(obj.uploadSQLStatement());
       } 
    }

}
