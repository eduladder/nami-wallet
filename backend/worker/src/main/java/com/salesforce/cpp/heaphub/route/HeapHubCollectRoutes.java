package com.salesforce.cpp.heaphub.route;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Stack;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Global;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.vo.heapdump.dominatortree.BaseRecord;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Enums;
import com.salesforce.cpp.heaphub.util.Response;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.metrics.Metrics;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;

public class HeapHubCollectRoutes extends HeapHubBaseRoute{

    class DomTreeObject extends BaseRecord {
        private int parentId;
        private String memLocation;

        public DomTreeObject() {
            super();
        }

        public DomTreeObject(JsonObject obj) {
            this.setGCRoot(obj.getBoolean(Constant.DomTree.GC_ROOT_KEY));
            this.setLabel(obj.getString(Constant.DomTree.LABEL_KEY));
            this.setObjectId(obj.getInteger(Constant.DomTree.OBJECT_ID_KEY));
            this.setObjectType(obj.getInteger(Constant.DomTree.OBJECT_TYPE_KEY));
            this.setPercent(obj.getDouble(Constant.DomTree.PERCENT_KEY));
            this.setRetainedSize(obj.getLong(Constant.DomTree.RETAINED_SIZE_KEY));
            this.setShallowSize(obj.getLong(Constant.DomTree.SHALLOW_SIZE_KEY));
            this.setSuffix(obj.getString(Constant.DomTree.SUFFIX_KEY));
            this.parentId = -1;
            String label = obj.getString(Constant.DomTree.LABEL_KEY);
            String[] splitLabel = label.split("@ ");
            if (splitLabel.length != 2) {
                throw new IllegalArgumentException("Invalid label: " + label);
            }
            this.setLabel(splitLabel[0]);
            this.setMemLocation(splitLabel[1]);
        }

        public DomTreeObject(JsonObject obj, int parentId) {
            this(obj);
            this.parentId = parentId;
        }


        public void setParentId(int parentId) {
            this.parentId = parentId;
        }

        public void setMemLocation(String memLocation) {
            this.memLocation = memLocation;
        }

        public int getParentId() {
            return parentId;
        }

        public String getMemLocation() {
            return memLocation;
        }

        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
            this.getLabel(), this.getMemLocation(), this.getObjectId(), this.getObjectType(), this.getParentId(), this.getPercent(), this.getRetainedSize(), this.getShallowSize(), this.getSuffix(), this.isGCRoot());
        }
    }

    @RouteMeta(path = "/collect/domtree", method = HttpMethod.GET)
    void collectDomTree(Future<JsonObject> future, RoutingContext context, @ParamKey("file") String file) {
    	
        HttpRequest<Buffer> request =
        CLIENT.request(HttpMethod.GET, Global.PORT, Global.HOST, uri("/heap-dump/2_eu35.hprof/dominatorTree/roots?page=1&pageSize=10&grouping=NONE"));
       
        request.send(
            ar -> {
            	JsonObject resp = null;
                if(ar.succeeded()) {
                	 resp = ar.result().bodyAsJsonObject();

                }
                future.complete(resp);
            }
        );    	
    }

    public ArrayList<DomTreeObject> collectDominatorRoots(String fileName, long minSize) {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);
            while(loop) {
                ArrayList<DomTreeObject> objs = getDomTreeRoots(fileName, i, 32);
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

    public ArrayList<DomTreeObject> getDomTreeRoots(String fileName, int page, int pageSize) throws ClientProtocolException, IOException, URISyntaxException {
        ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);

        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + fileName + "/dominatorTree/roots")
		.addParameter("grouping", "NONE")
		.addParameter("page", String.valueOf(page))
		.addParameter("pageSize", String.valueOf(pageSize))
   		.build();
		HttpGet getDomTreeRoots = new HttpGet(uri);
        // Cannot use Vert-X because need synchronouse client
        Response res = new Response(CLIENT_SYNC.execute(getDomTreeRoots));
        if (res.getStatusCode() >= 300) {
            System.out.print("Request Failed");
            return null;
        }
        JsonObject resJSON = res.getBody();
        JsonArray jSONArray = resJSON.getJsonArray("data");
        if (jSONArray == null) {
            return null;
        }
        int i = 0;
        // get first index in array
        JsonObject curr = jSONArray.getJsonObject(i);
        if (curr == null) {
            System.out.println("curr is null");
            return null;
        }
        while (curr != null) {
            // print now until we decide how to store data
            DomTreeObject obj = new DomTreeObject(curr);
            arr.add(obj);
            i++;
            curr = jSONArray.getJsonObject(i);
        }
        return arr;
    }

    public ArrayList<DomTreeObject> getDomTreeChildren(String fileName, int page, int pageSize, int parentId) throws ClientProtocolException, IOException, URISyntaxException {
            // call api
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + fileName + "/dominatorTree/children")
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
                    System.out.println("resJSON is null");
                    // TODO: throw exception
                    return null;
                }
                // get data attribute of json body
                JsonArray jSONArray = resJSON.getJsonArray("data");
                if (jSONArray == null) {
                    System.out.println("resJSON.data is null");
                    // TODO: throw exception
                    return null;
                }
                int i = 0;
                // get first index in array
                // System.out.println(resJSON.get(i));
                JsonObject curr = jSONArray.getJsonObject(i);
                if (curr == null) {
                    System.out.println("curr is null");
                    return null;
                }
                ArrayList<DomTreeObject> arr = new ArrayList<DomTreeObject>(32);
                while (curr != null) {
                    // print now until we decide how to store data
                    DomTreeObject obj = new DomTreeObject(curr, parentId);
                    arr.add(obj);
                    i++;
                    curr = jSONArray.getJsonObject(i);
                    // produce to next object in array
                }
                return arr;
            }
        }

    
    private ArrayList<DomTreeObject> collectDominatorChildren(String fileName, int parentId, int branchingFactor) {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<DomTreeObject> output = new ArrayList<DomTreeObject>(128);
            int cnt = 0;
            while(loop) {
                ArrayList<DomTreeObject> objs = getDomTreeChildren(fileName, i, 32, parentId);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (DomTreeObject obj : objs) {
                // if (dominatorTree.containsKey(obj.getLabel())) {
                //     System.out.println("DUPLICATE KEY: " + obj.getLabel());
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

    public  ArrayList<DomTreeObject> getDomTree(String fileName, ArrayList<DomTreeObject> roots, long minSize, int branchingFactor, int maxDepth) {
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
                for (DomTreeObject obj : collectDominatorChildren(fileName, curr.obj.getObjectId(), branchingFactor)) {
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
    
}
