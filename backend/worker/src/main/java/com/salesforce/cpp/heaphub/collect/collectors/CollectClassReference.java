package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Stack;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;

import com.salesforce.cpp.heaphub.collect.models.ClassHistoInfo;
import com.salesforce.cpp.heaphub.collect.models.ClassReference;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Collector class to collect all outgoing and incoming object references for each class in the heap dump's histogram and upload the results to SQL. Since this is a tree data structure, the data collected is restricted by the passed in maximum depth and branching factor.
 */
public class CollectClassReference extends CollectBase{

    private int heapId;
    private long createdAt;
    private ArrayList<ClassHistoInfo> histogram;
    private int maxDepth;
    private int branchingFactor;
    private String heapName;
    
    public CollectClassReference(int heapId, String heapName, long createdAt, ArrayList<ClassHistoInfo> histogram, int maxDepth, int branchingFactor) {
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.histogram = histogram;
        this.maxDepth = maxDepth;
        this.branchingFactor = branchingFactor;
        this.heapName = heapName;
    }

    /***
     * For a given class/objectId, make a request to the JIFA api to get all the outgoing references for the class/object
     * @param page
     * @param pageSize
      * @param objectId
     * @return ArrayList<ClassReference> which contains all the outgoing references for a given objectId
     */
    public ArrayList<ClassReference> getOutbounds(int page, int pageSize, int objectId){
        try {
            // make request to JIFA API
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/outbounds")
            .addParameter("objectId", String.valueOf(objectId))
            .addParameter("page", String.valueOf(page))
            .addParameter("pageSize", String.valueOf(pageSize))
            .build();
            HttpGet getOutbounds = new HttpGet(uri);
            Response res = new Response(CLIENT_SYNC.execute(getOutbounds));
            if (res.getStatusCode() >= 300) {
                return null;
            } else {
                // Parse result into desired class format of ClassReference
                JsonObject resJSON = res.getBody();
                if (resJSON == null) {
                    return null;
                }
                JsonArray jsonArray = resJSON.getJsonArray("data");
                if (jsonArray == null) {
                    log("resJSON.data is null");
                    return null;
                }
                ArrayList<ClassReference> arr = new ArrayList<ClassReference>(32);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new ClassReference(curr, heapId, createdAt, objectId, false));
                }
                return arr;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }

    /***
     * For a given class/objectId, make a request to the JIFA api to get all the incoming references for the class/object
     * @param page
     * @param pageSize
     * @param objectId
     * @return ArrayList<ClassReference> which contains all the incoming references for a given objectId
     */
    public ArrayList<ClassReference> getInbounds(int page, int pageSize, int objectId) {
        try {
            // make request to JIFA API
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/inbounds")
            .addParameter("objectId", String.valueOf(objectId))
            .addParameter("page", String.valueOf(page))
            .addParameter("pageSize", String.valueOf(pageSize))
            .build();
            HttpGet getOutbounds = new HttpGet(uri);
            Response res = new Response(CLIENT_SYNC.execute(getOutbounds));
            if (res.getStatusCode() >= 300) {
                return null;
            } else {
                // Parse result into desired class format of ClassReference
                JsonObject resJSON = res.getBody();
                if (resJSON == null) {
                    return null;
                }
                JsonArray jsonArray = resJSON.getJsonArray("data");
                if (jsonArray == null) {
                    log("resJSON.data is null");
                    return null;
                }
                ArrayList<ClassReference> arr = new ArrayList<ClassReference>(32);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new ClassReference(curr, heapId, createdAt, objectId, true));
                }
                return arr;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }


    /***
      * For a given objectId/classId, get all its inbounds and outbounds. The amount of inbounds/outbounds returned is limited by the branchingFactor.
      * @param parentId
      * @return ArrayList<ClassReference> which contains all the inbounds for a given objectId
      */
    private ArrayList<ClassReference> collectSingleObjectBounds(int parentId) {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<ClassReference> output = new ArrayList<ClassReference>(128);
            int cnt = 0;
            while(loop) {
                ArrayList<ClassReference> objs = getOutbounds(i, 32, parentId);
                // if we have collected all outbounds for the current object break
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (ClassReference obj : objs) {
                    output.add(obj);
                    cnt++;
                    // collect object until branching factor is reached -> break and exit collection loop
                    if (cnt >= branchingFactor) {
                        loop = false;
                        break;
                    }
                }
                i++;
                // if we did not fill the page size, we have collected all outbounds for the current object -> break
                if (objs.size() < 32) {
                    break;
                }
            }
            i = 1;
            loop = true;
            cnt = 0;
            while(loop) {
                ArrayList<ClassReference> objs = getInbounds(i, 32, parentId);
                // if we have collected all inbounds for the current object break
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (ClassReference obj : objs) {
                    output.add(obj);
                    cnt++;
                    // collect object until branching factor is reached -> break and exit collection loop
                    if (cnt >= branchingFactor) {
                        loop = false;
                        break;
                    }
                }
                i++;
                // if we did not fill the page size, we have collected all outbounds for the current object -> break
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


    /***
      * Collect all the inbounds and outbounds for every class in the histogram
      * @param objectId
      * @return ArrayList<ClassReference> - list of all objects which are inbounds or outbounds for any class in the histogram 
       */
    public  ArrayList<ClassReference> collectBounds() {
        // Stack object class to avoid using recursion
        class StackObjInfo {
            public ClassReference obj;
            public int depth;
            public StackObjInfo(ClassReference obj, int depth) {
                this.obj = obj;
                this.depth = depth;
            }
        }
        // Conduct a DFS traversal of the histogram to collect all layers of inbounds and outbounds
        Stack<StackObjInfo> stack = new Stack<StackObjInfo>();
        // Initiate DFS by adding all initial children of histogram to stack
        for (ClassHistoInfo curr : histogram) {
            ArrayList<ClassReference> children = collectSingleObjectBounds(curr.getObjectId());
            for (ClassReference child : children) {
                stack.push(new StackObjInfo(child, 1));
            }
        }
    
        ArrayList<ClassReference> output = new ArrayList<ClassReference>();        
        // DFS traversal
        while (!stack.isEmpty()) {
            StackObjInfo curr = stack.pop();
            output.add(curr.obj);
            // limit collection to maxDepth for efficiency
            if (curr.depth < maxDepth) {
                for (ClassReference obj : collectSingleObjectBounds(curr.obj.getObjectId())) {
                    stack.push(new StackObjInfo(obj, curr.depth + 1));
                }
            }
        }
        return output;
    }

    /**
     * Collect all the inbounds and outbounds for every class in the histogram and then upload the collected references to a SQL database.
     * @throws IOException
     */
    public void collectAndUpload() throws IOException {
        ArrayList<ClassReference> arr = collectBounds();
        // Use SQL batch insert (with batch size of 100) to upload all data to database
        StringBuilder sb = new StringBuilder(ClassReference.uploadSQLStatement());
        int cnt = 0;
        for (ClassReference obj : arr) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(obj.getSQLValues());
            cnt++;
            // set batch size
            if (cnt == 100) {
                sb.append(";");
                driver.executeUpdate(sb.toString());
                sb = new StringBuilder(ClassReference.uploadSQLStatement());
                cnt = 0;
            }
        }
        if (cnt != 0) {
            sb.append(";");
            driver.executeUpdate(sb.toString());
        }
    }

}
