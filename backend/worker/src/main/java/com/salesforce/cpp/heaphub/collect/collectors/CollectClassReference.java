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
import com.salesforce.cpp.heaphub.collect.models.DomTreeObject;
import com.salesforce.cpp.heaphub.collect.models.Outbounds;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectClassReference extends CollectBase{

    private int heapId;
    private long createdAt;
    private ArrayList<ClassHistoInfo> histogram;
    private int maxDepth;
    private int branchingFactor;
    String heapName;
    
    public CollectClassReference(int heapId, String heapName, long createdAt, ArrayList<ClassHistoInfo> histogram, int maxDepth, int branchingFactor) {
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.histogram = histogram;
        this.maxDepth = maxDepth;
        this.branchingFactor = branchingFactor;
        this.heapName = heapName;
    }

    public ArrayList<ClassReference> getOutbounds(int page, int pageSize, int objectId){
        try {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/outbounds")
            .addParameter("objectId", String.valueOf(objectId))
            .addParameter("page", String.valueOf(page))
            .addParameter("pageSize", String.valueOf(pageSize))
            .build();
            HttpGet getOutbounds = new HttpGet(uri);
            Response res = new Response(CLIENT_SYNC.execute(getOutbounds));
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
                ArrayList<ClassReference> arr = new ArrayList<ClassReference>(32);
                // TODO: update with proper return format
                for (int i = 0; i < jsonArray.size(); i++) {
                    // print now until we decide how to store data
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new ClassReference(curr, heapId, createdAt, objectId, false));
                    // produce to next object in array
                }
                return arr;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }

    public ArrayList<ClassReference> getInbounds(int page, int pageSize, int objectId) {
        try {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/inbounds")
            .addParameter("objectId", String.valueOf(objectId))
            .addParameter("page", String.valueOf(page))
            .addParameter("pageSize", String.valueOf(pageSize))
            .build();
            HttpGet getOutbounds = new HttpGet(uri);
            Response res = new Response(CLIENT_SYNC.execute(getOutbounds));
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
                ArrayList<ClassReference> arr = new ArrayList<ClassReference>(32);
                // TODO: update with proper return format
                for (int i = 0; i < jsonArray.size(); i++) {
                    // print now until we decide how to store data
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new ClassReference(curr, heapId, createdAt, objectId, true));
                    // produce to next object in array
                }
                return arr;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }

    private ArrayList<ClassReference> collectSingleObjectBounds(int parentId) {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<ClassReference> output = new ArrayList<ClassReference>(128);
            int cnt = 0;
            while(loop) {
                ArrayList<ClassReference> objs = getOutbounds(i, 32, parentId);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (ClassReference obj : objs) {
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
            i = 1;
            loop = true;
            cnt = 0;
            while(loop) {
                ArrayList<ClassReference> objs = getInbounds(i, 32, parentId);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (ClassReference obj : objs) {
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

    public  ArrayList<ClassReference> collectBounds() {
        class StackObjInfo {
            public ClassReference obj;
            public int depth;
            public StackObjInfo(ClassReference obj, int depth) {
                this.obj = obj;
                this.depth = depth;
            }
        }
    
        Stack<StackObjInfo> stack = new Stack<StackObjInfo>();
        for (ClassHistoInfo curr : histogram) {
            ArrayList<ClassReference> children = collectSingleObjectBounds(curr.getObjectId());
            for (ClassReference child : children) {
                stack.push(new StackObjInfo(child, 1));
            }
        }
    
        ArrayList<ClassReference> output = new ArrayList<ClassReference>();        
        
        while (!stack.isEmpty()) {
            StackObjInfo curr = stack.pop();
            output.add(curr.obj);
            if (curr.depth < maxDepth) {
                for (ClassReference obj : collectSingleObjectBounds(curr.obj.getObjectId())) {
                    stack.push(new StackObjInfo(obj, curr.depth + 1));
                }
            }
        }
        return output;
    }

    public void collectAndUpload() throws IOException {
        ArrayList<ClassReference> arr = collectBounds();
        StringBuilder sb = new StringBuilder(ClassReference.uploadSQLStatement());
        int cnt = 0;
        for (ClassReference obj : arr) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(obj.getSQLValues());
            cnt++;
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
