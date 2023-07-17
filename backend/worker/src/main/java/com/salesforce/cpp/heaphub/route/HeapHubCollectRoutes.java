package com.salesforce.cpp.heaphub.route;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Global;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.vo.heapdump.dominatortree.BaseRecord;
import org.eclipse.jifa.worker.vo.heapdump.histogram.Record;
import org.json.JSONArray;

import com.fasterxml.jackson.databind.JsonNode;
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

    class ClassHistoInfo {
        private String memLocation;
        private String label;
        private long numberOfObjects;
        private long shallowSize;
        private long retainedSize;
        private int objectId;
        private int type;

        public ClassHistoInfo(JsonObject obj) {
            String label = obj.getString(Constant.Histogram.LABEL_KEY);
            String[] splitLabel = label.split("@ ");
            if (splitLabel.length != 2) {
                throw new IllegalArgumentException("Invalid label: " + label);
            }
            this.label = splitLabel[0];
            this.memLocation = splitLabel[1];
            this.numberOfObjects = obj.getLong(Constant.Histogram.NUM_OBJECTS_KEY);
            this.shallowSize = obj.getLong(Constant.Histogram.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Histogram.RETAINED_SIZE_KEY);
            this.objectId = obj.getInteger(Constant.Histogram.OBJECT_ID_KEY);
            this.type = obj.getInteger(Constant.Histogram.TYPE_KEY);
        }

        public String getMemLocation() {
            return memLocation;
        }

        public String getLabel() {
            return label;
        }
        public long getNumberOfObjects() {
            return numberOfObjects;
        }
        public long getShallowSize() {
            return shallowSize;
        }
        public long getRetainedSize() {
            return retainedSize;
        }
        public int getObjectId() {
            return objectId;
        }
        public int getType() {
            return type;
        }

        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s\n",
            this.getLabel(), this.getMemLocation(), this.getObjectId(), this.getRetainedSize(), this.getShallowSize(), this.getNumberOfObjects(), this.getType());
        }

    }

    class ThreadInfo {
        private int objectId;

        private String object;
    
        private String name;
    
        private long shallowSize;
    
        private long retainedSize;
    
        private String contextClassLoader;
    
        private boolean hasStack;
    
        private boolean daemon;

        public ThreadInfo(JsonObject obj) {
            this.objectId = obj.getInteger(Constant.Threads.OBJECT_ID_KEY);
            this.object = obj.getString(Constant.Threads.OBJECT_KEY);
            this.name = obj.getString(Constant.Threads.NAME_KEY);
            this.shallowSize = obj.getLong(Constant.Threads.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Threads.RETAINED_SIZE_KEY);
            this.contextClassLoader = obj.getString(Constant.Threads.CONTEXT_CLASS_LOADER_KEY);
            this.hasStack = obj.getBoolean(Constant.Threads.HAS_STACK_KEY);
            this.daemon = obj.getBoolean(Constant.Threads.DAEMON_KEY);
        }

        public int getObjectId() {
            return objectId;
        }

        public String getObject() {
            return object;
        }
        public String getName() {
            return name;
        }
        public long getShallowSize() {
            return shallowSize;
        }
        public long getRetainedSize() {
            return retainedSize;
        }
        public String getContextClassLoader() {
            return contextClassLoader;
        }
        public boolean hasStack() {
            return hasStack;
        }
        public boolean isDaemon() {
            return daemon;
        }
        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
            this.getObjectId(), this.getObject(), this.getName(), this.getShallowSize(), this.getRetainedSize(), this.getContextClassLoader(), this.hasStack(), this.isDaemon());
        }
            
    }

    class StackFrame {
        private String stack;
        private boolean hasLocal;
        private int stackId;
    
    
        public StackFrame(String stack, boolean hasLocal, int stackId) {
            this.stack = stack;
            this.hasLocal = hasLocal;
            this.stackId = stackId;
        }

        public StackFrame(JsonObject obj) {
            this.stack = obj.getString(Constant.StackFrame.STACK_NAME_KEY);
            this.hasLocal = obj.getBoolean(Constant.StackFrame.HAS_LOCAL_KEY);
            this.stackId = obj.getInteger(Constant.StackFrame.STACK_ID_KEY);
        }
        
        public String toCSV() {
            return String.format("%s,%s,%s\n",
            this.getStack(), this.isHasLocal(), this.getStackId());
        }

        public String getStack() {
            return stack;
        }
        public boolean isHasLocal() {
            return hasLocal;
        }

        public int getStackId() {
            return stackId;
        }
    }
    
    class Outbounds {
        private int objectId;
        private String prefix;
        private String label;
        private String suffix;
        private long shallowSize;
        private long retainedSize;
        private boolean hasInbound;
        private boolean hasOutbound;
        private int objectType;
        private boolean gCRoot;
        private int parentId;

        public Outbounds(JsonObject obj, int parentId) {
            this.objectId = obj.getInteger(Constant.Outbounds.OBJECT_ID_KEY);
            this.prefix = obj.getString(Constant.Outbounds.PREFIX_KEY);
            this.label = obj.getString(Constant.Outbounds.LABEL_KEY);
            this.suffix = obj.getString(Constant.Outbounds.SUFFIX_KEY);
            this.shallowSize = obj.getLong(Constant.Outbounds.SHALLOW_SIZE_KEY);
            this.retainedSize = obj.getLong(Constant.Outbounds.RETAINED_SIZE_KEY);
            this.hasInbound = obj.getBoolean(Constant.Outbounds.HAS_INBOUND_KEY);
            this.hasOutbound = obj.getBoolean(Constant.Outbounds.HAS_OUTBOUND_KEY);
            this.objectType = obj.getInteger(Constant.Outbounds.OBJECT_TYPE_KEY);
            this.gCRoot = obj.getBoolean(Constant.Outbounds.GC_ROOT_KEY);
            this.parentId = parentId;
        }

        public int getObjectId() {
            return objectId;
        }

        public String getPrefix() {
            return prefix;
        }
        public String getLabel() {
            return label;
        }
        public String getSuffix() {
            return suffix;
        }
        public long getShallowSize() {
            return shallowSize;
        }
        public long getRetainedSize() {
            return retainedSize;
        }
        public boolean isHasInbound() {
            return hasInbound;
        }
        public boolean isHasOutbound() {
            return hasOutbound;
        }
        public int getObjectType() {
            return objectType;
        }
        public boolean isGCRoot() {
            return gCRoot;
        }
        public int getParentId() {
            return parentId;
        }
        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", objectId, prefix, label, suffix, shallowSize, retainedSize, hasInbound, hasOutbound, objectType, gCRoot,parentId);
        }
    }
    
    class PathToGCRootElement {
        private int objectId;
        private int parentId;
        private String label;
        private String memoryLocation;
        private boolean origin;
        private String prefix;
        private String suffix;
        private int shallowSize;
        private int retainedSize;
        private boolean hasInbound;
        private boolean hasOutbound;
        private int objectType;
        private boolean gCRoot;

        public PathToGCRootElement(JsonObject obj, int parentId) {
            if (obj.getString("suffix") == null) {
                suffix = "";
            } else {
                suffix = obj.getString("suffix");
            }

            if (obj.getString("prefix") == null) {
                prefix = "";
            } else {
                prefix = obj.getString("prefix");
            }

            if (obj.getBoolean("hasInbound") == null) {
                hasInbound = false;
            } else {
                hasInbound = obj.getBoolean("hasInbound");
            }

            if (obj.getBoolean("hasOutbound") == null) {
                hasOutbound = false;
            } else {
                hasOutbound = obj.getBoolean("hasOutbound");
            }

            if (obj.getBoolean("GCRoot") == null) {
                gCRoot = false;
            } else {
                gCRoot = obj.getBoolean("GCRoot");
            }

            this.parentId = parentId;
            objectId = obj.getInteger("objectId");
            label = obj.getString("label");
            shallowSize = obj.getInteger("shallowSize");
            retainedSize = obj.getInteger("retainedSize");
            objectType = obj.getInteger("objectType");
        }
        
        public int getObjectId() {
            return objectId;
        }

        public int parentId() {
            return parentId;
        }

        public String label() {
            return label;
        }
        
        public String memoryLocation() {
            return memoryLocation;
        }
        
        public boolean origin() {
            return origin;
        }
        
        public String suffix() {
            return suffix;
        }
        
        public String prefix() {
            return prefix;
        }
        
        public boolean hasInbound() {
            return hasInbound;
        }
        
        public boolean hasOutbound() {
            return hasOutbound;
        }
        
        public boolean gCRoot() {
            return gCRoot;
        }
        
        public int shallowSize() {
            return shallowSize;
        }
        
        public int retainedSize() {
            return retainedSize;
        }
        
        public int objectType() {
            return objectType;
        }
        
        public String toCSV() {
            return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    parentId, objectId, label, memoryLocation, origin, suffix, prefix, hasInbound, hasOutbound, gCRoot, shallowSize, retainedSize, objectType);
        }

    }


    class GCRootPath {
        private List<PathToGCRootElement> path;
        private boolean hasMore;

        public GCRootPath(List<PathToGCRootElement> path, boolean hasMore) {
            this.path = path;
            this.hasMore = hasMore;
        }
        
        public List<PathToGCRootElement> getPath() {
            return path;
        }
        
        public boolean hasMore() {
            return hasMore;
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

    public  ArrayList<DomTreeObject> collectDomTree(String fileName, ArrayList<DomTreeObject> roots, long minSize, int branchingFactor, int maxDepth) {
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

    public ArrayList<ClassHistoInfo> collectHistogram(String heapName, long minSize) {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<ClassHistoInfo> arr = new ArrayList<ClassHistoInfo>(32);
            while(loop) {
                ArrayList<ClassHistoInfo> objs = getHistogramRoots(heapName, i, 32);
                // System.out.println(objs);
                if (objs == null || objs.size() == 0) {
                    break;
                }
                for (ClassHistoInfo obj : objs) {
                    if (obj.getShallowSize() < minSize) {
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

    public ArrayList<ClassHistoInfo> getHistogramRoots(String fileName, int page, int pageSize) {
        try {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + fileName + "/histogram")
            .addParameter("groupingBy", "BY_CLASS")
            .addParameter("page", String.valueOf(page))
            .addParameter("pageSize", String.valueOf(pageSize))
            .build();
            HttpGet getDomTreeRoots = new HttpGet(uri); 
            Response res = new Response(CLIENT_SYNC.execute(getDomTreeRoots));
            // System.out.println("SPOT 2");
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
                    System.out.println("resJSON.data is null");
                    // TODO: throw exception
                    return null;
                }
                int i = 0;
                // get first index in array
                JsonObject curr = jsonArray.getJsonObject(i);
                if (curr == null) {
                    System.out.println("curr is null");
                    return null;
                }
                System.out.println(curr);
                ArrayList<ClassHistoInfo> arr = new ArrayList<ClassHistoInfo>(32);
                // TODO: update with proper return format
                while (curr != null) {
                    arr.add(new ClassHistoInfo(curr));
                    i++;
                    curr = jsonArray.getJsonObject(i);
                    // produce to next object in array
                }
                return arr;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }
    
    public ArrayList<ThreadInfo> collectThreads(String heapName, long minSize) {
        try {
            int i = 1;
            boolean loop = true;
            ArrayList<ThreadInfo> arr = new ArrayList<ThreadInfo>(32);
            while(loop) {
                ArrayList<ThreadInfo> objs = getThreads(heapName, i, 32);
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
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<ThreadInfo> getThreads(String fileName, int page, int pageSize) throws ClientProtocolException, IOException, URISyntaxException {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + fileName + "/threads")
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
                    System.out.println("resJSON is null");
                    // TODO: throw exception
                    return null;
                }
                // get data attribute of json body
                JsonArray jsonArray = resJSON.getJsonArray("data");
                if (jsonArray == null) {
                    System.out.println("resJSON.data is null");
                    // TODO: throw exception
                    return null;
                }
                int i = 0;
                // get first index in array
                JsonObject curr = jsonArray.getJsonObject(i);
                if (curr == null) {
                    System.out.println("curr is null");
                    return null;
                }
                // TODO: update with proper return format
                ArrayList<ThreadInfo> arr = new ArrayList<ThreadInfo>(32);
                while (curr != null) {
                    ThreadInfo thread = new ThreadInfo(curr);
                    i++;
                    curr = jsonArray.getJsonObject(i);
                    arr.add(thread);
                }
                return arr;
            }
        }

    public ArrayList<StackFrame> collectStackTraces(String heapName,ArrayList<ThreadInfo> threads) {
        try {
            ArrayList<StackFrame> acc = new ArrayList<StackFrame>();
            for (ThreadInfo thread : threads) {
                ArrayList<StackFrame> stacktrace = getStackTrace(heapName, thread.getObjectId());
                acc.addAll(stacktrace);
            }
        return acc;
        } catch (Exception e) {
        return null;
        }
    }

    public ArrayList<StackFrame> getStackTrace(String heapName, int threadId) throws ClientProtocolException, IOException, URISyntaxException {
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/threads")
		.addParameter("objectId", String.valueOf(threadId))
		.build();
		HttpGet getStackTrace = new HttpGet(uri);
        Response res = new Response(CLIENT_SYNC.execute(getStackTrace));
        if (res.getStatusCode() >= 300) {
            // TODO: throw exception
            return null;
        } else {
            // create json object with res body
            JsonArray resJSON = new JsonArray(res.getBody().encode());
            if (resJSON == null) {
                System.out.println("resJSON is null");
                // TODO: throw exception
                return null;
            }                
            int i = 0;
            // get first index in array
            JsonObject curr = resJSON.getJsonObject(i);
            if (curr == null) {
                System.out.println("curr is null");
                return null;
            }
            // TODO: update with proper return format
            ArrayList<StackFrame> arr = new ArrayList<StackFrame>(32);
            while (curr != null) {
                StackFrame stack = new StackFrame(curr);
                i++;
                curr = resJSON.getJsonObject(i);
                arr.add(stack);
            }
            return arr;
        }

    }

    private ArrayList<Outbounds> collectSingleRootOutbounds(String heapName, DomTreeObject root, int max) {
        try {
            int i = 1;
            int cnt = 0;
            int id = root.getObjectId();
            boolean loop = true;
            ArrayList<Outbounds> arr = new ArrayList<Outbounds>(32);
            while(loop) {
                ArrayList<Outbounds> obs = getOutbounds(heapName, id, i, 32);
                if (obs == null || obs.size() == 0) {
                    break;
                }
                for (Outbounds ob : obs) {
                    if (cnt >= max) {
                        loop = false;
                        break;
                    } else {
                        arr.add(ob);
                        cnt++;
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

    public ArrayList<Outbounds> getOutbounds(String fileName, int objectId, int page, int pageSize){
        try {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + fileName + "/outbounds")
            .addParameter("objectId", String.valueOf(objectId))
            .addParameter("page", String.valueOf(page))
            .addParameter("pageSize", String.valueOf(pageSize))
            .build();
            HttpGet getStackTrace = new HttpGet(uri);
            Response res = new Response(CLIENT_SYNC.execute(getStackTrace));
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
                    System.out.println("resJSON.data is null");
                    // TODO: throw exception
                    return null;
                }
                int i = 0;
                // get first index in array
                JsonObject curr = jsonArray.getJsonObject(i);
                if (curr == null) {
                    System.out.println("curr is null");
                    return null;
                }
                ArrayList<Outbounds> arr = new ArrayList<Outbounds>(32);
                // TODO: update with proper return format
                while (curr != null) {
                    // print now until we decide how to store data
                    arr.add(new Outbounds(curr, objectId));
                    i++;
                    curr = jsonArray.getJsonObject(i);
                    // produce to next object in array
                }
                return arr;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }

    public ArrayList<Outbounds> collectRootOutbounds(String heapName, ArrayList<DomTreeObject> roots, int max) {
        try {
            ArrayList<Outbounds> acc = new ArrayList<Outbounds>();
            for (DomTreeObject root : roots) {
                ArrayList<Outbounds> stacktrace = collectSingleRootOutbounds(heapName, root, max);
                acc.addAll(stacktrace);
            }
            return acc;
        } catch (Exception e) {
            return null;
        }
    }

    public ArrayList<PathToGCRootElement> collectPathsToGCRoots(String heapName, ArrayList<DomTreeObject> roots, FileOutputStream fos) {
        try {
            ArrayList<PathToGCRootElement> acc = new ArrayList<PathToGCRootElement>();
            for (DomTreeObject root : roots) {
                ArrayList<PathToGCRootElement> path = collectGCPath(heapName, root, fos);
                acc.addAll(path);
            }
            return acc;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    
    public ArrayList<PathToGCRootElement> collectGCPath(String heapName, DomTreeObject root, FileOutputStream fos) throws IOException {
            ArrayList<PathToGCRootElement> acc = new ArrayList<PathToGCRootElement>();
            int i = 0;
            GCRootPath curr = getPathToGCRoots(heapName, root.getObjectId(), 0, 10);
            acc.addAll(curr.getPath());
            while (curr.hasMore() && i < 10) {
                curr = getPathToGCRoots(heapName, root.getObjectId(), 10 * i, 10);
                acc.addAll(curr.getPath());
                i++;
            }
            return acc;
    }

    public GCRootPath getPathToGCRoots(String fileName, int originId, int skip, int count)
    {
    try {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + fileName + "/pathToGCRoots")
            .addParameter("origin", String.valueOf(originId))
            .addParameter("skip", String.valueOf(skip))
            .addParameter("count", String.valueOf(count))
            .build();
            HttpGet mergePathToGCRootsByClassId = new HttpGet(uri);
            Response res = new Response(CLIENT_SYNC.execute(mergePathToGCRootsByClassId));
            if (res.getStatusCode() >= 300) {
                // TODO
                System.out.println("res.getStatusCode() >= 300");
                return null;
            } else {
                JsonObject resJSON = res.getBody();
                if (resJSON == null) {
                    // TODO
                    System.out.println("resJSON is null");
                    return null;
                }

                JsonObject tree = resJSON.getJsonObject("tree");
                if (tree == null) {
                    System.out.println("resJSON.tree is null");
                    // TODO: throw exception
                    return null;
                }
                // get first index in array
                ArrayList<PathToGCRootElement> pathList = collectTree(tree);
                GCRootPath path = new GCRootPath(pathList, resJSON.getBoolean("hasMore"));
                return path;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }

    public ArrayList<PathToGCRootElement> collectTree(JsonObject src) throws Exception {
        class StackObjInfo {
            public JsonObject node;
            public int parentId;
            public StackObjInfo(JsonObject node, int parentId) {
                this.node = node;
                this.parentId = parentId;
            }
        }
    
        Stack<StackObjInfo> stack = new Stack<StackObjInfo>();
        stack.push(new StackObjInfo(src, -1));
        
        ArrayList<PathToGCRootElement> output = new ArrayList<PathToGCRootElement>();        
    
        while (!stack.isEmpty()) {
            StackObjInfo curr = stack.pop();
            output.add(new PathToGCRootElement(curr.node, curr.parentId));
            int i = 0;
            JsonArray children = curr.node.getJsonArray("children");
            while (children.getJsonObject(i) != null) {
                stack.push(new StackObjInfo(children.getJsonObject(i), curr.node.getInteger("objectId")));
                i++;
            }
        }
        return output;
    }

}
