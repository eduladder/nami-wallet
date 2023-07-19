package com.salesforce.cpp.heaphub.route;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.route.ParamKey;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.eclipse.jifa.worker.vo.heapdump.dominatortree.BaseRecord;

import com.salesforce.cpp.heaphub.util.Response;

import org.eclipse.jifa.worker.Constant;

import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class HeapHubCollectRoutes extends HeapHubBaseRoute{

    String logFilePath = "/Users/dbarra/git/heaphub/outputs/log.txt";

    File logFile = new File(logFilePath);

    void log(Object o) {
        try {
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write((o.toString()+"\n").getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class DomTreeObject extends BaseRecord {
        private int parentId; // done
        private String memLocation; // done
        // created_at missing
        // update_at mising
        // heap_id - missing 
        // origin - missing - is this equivalent to deciding if root or not?
        // has inbound and has outbound are not present in returned json
        // prefix does not exist
        // private double percent; // not included in the stored information

        public DomTreeObject() {
            super();
        }

        public DomTreeObject(JsonObject obj) {
            // log("____creating dom tree object___");
            // log(obj.encodePrettily());
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
        // missing heap_id
        // created_at missing
        // updated_at missing
        private String memLocation;
        private String label; // done
        private long numberOfObjects; // done
        private long shallowSize; // done
        private long retainedSize; // done
        private int objectId; // done
        private int type; // done


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
        // add heap_id, created_at, updated_at
        private int objectId; // done
        private String object; // done object_label
        private String name; // done
        private long shallowSize; // done
        private long retainedSize; // done
        private String contextClassLoader; // done
        private boolean hasStack; // done
        private boolean daemon; // done

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
        // this needs to be distinguished from outbounds
        // missing created_at, updated_at, thread_id
        // what is object_id, object_label, prefix, suffiz, hasinbound, hasoutbound, shallowsize, retainedsize, gcroot - is this for outbounds
        private String stack; // done
        private boolean hasLocal; // done
        private boolean firstNonNativeFrame; // done

        public StackFrame(JsonObject obj) {
            this.stack = obj.getString(Constant.StackFrame.STACK_NAME_KEY);
            this.hasLocal = obj.getBoolean(Constant.StackFrame.HAS_LOCAL_KEY);
            this.firstNonNativeFrame = obj.getBoolean(Constant.StackFrame.FIRST_NON_NATIVE_FRAME_KEY);
        }
        
        public String toCSV() {
            return String.format("%s,%s,%s\n",
            this.getStack(), this.isHasLocal(), this.isFirstNonNativeFrame());
        }

        public String getStack() {
            return stack;
        }
        public boolean isHasLocal() {
            return hasLocal;
        }

        public boolean isFirstNonNativeFrame() {
            return firstNonNativeFrame;
        }
    }
    
    class Outbounds {
        // needs to be distinguished from stack frame
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
        private int parentId; // currently not being included

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
        // absolutely missing...
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

    // Test Route: 
    // http://localhost:8102/jifa-api/heaphub/2_eu35.hprof/collect?dominatorMinSize=50000000&maxDepth=2&branchingFactor=10&maxOutbounds=10&histoMinSize=10000000&threadMinSize=10000000

    @RouteMeta(path = "/collect", method = HttpMethod.GET)
    void collect(Future<JsonObject> future, RoutingContext context, @ParamKey("file") String file, @ParamKey("dominatorMinSize") long dominatorMinSize, @ParamKey("branchingFactor") int branchingFactor, @ParamKey("maxDepth") int maxDepth, @ParamKey("maxOutbounds") int maxOutbounds, @ParamKey("histoMinSize") long histoMinSize, @ParamKey("threadMinSize") long threadMinSize) throws Exception{
        String dest = "/Users/dbarra/git/heaphub/outputs";
        collectAsCSV(file, dest, dominatorMinSize, branchingFactor, maxDepth, maxOutbounds, histoMinSize, threadMinSize);
        future.complete(new JsonObject("{\"success\": \"true\"}"));
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
            // log("Collect Dominator Roots: " + arr);
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
            DomTreeObject obj = new DomTreeObject(curr);
            arr.add(obj);
        }
        // log("getDomTree Return: " + arr);
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
                    DomTreeObject obj = new DomTreeObject(curr, parentId);
                    arr.add(obj);
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
            HttpGet getHistoRoots = new HttpGet(uri); 
            Response res = new Response(CLIENT_SYNC.execute(getHistoRoots));
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
                ArrayList<ClassHistoInfo> arr = new ArrayList<ClassHistoInfo>(32);

                for(int i = 0; i < jsonArray.size(); i++) {
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new ClassHistoInfo(curr));

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
                    ThreadInfo thread = new ThreadInfo(curr);
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
        URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/stackTrace")
		.addParameter("objectId", String.valueOf(threadId))
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
            ArrayList<StackFrame> arr = new ArrayList<StackFrame>(32);
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject curr = jsonArray.getJsonObject(i);
                StackFrame stack = new StackFrame(curr);
                arr.add(stack);
                i++;
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
                ArrayList<Outbounds> arr = new ArrayList<Outbounds>(32);
                // TODO: update with proper return format
                for (int i = 0; i < jsonArray.size(); i++) {
                    // print now until we decide how to store data
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new Outbounds(curr, objectId));
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
                log("res.getStatusCode() >= 300");
                return null;
            } else {
                JsonObject resJSON = res.getBody();
                if (resJSON == null) {
                    // TODO
                    log("resJSON is null");
                    return null;
                }

                JsonObject tree = resJSON.getJsonObject("tree");
                if (tree == null) {
                    log("resJSON.tree is null");
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

    public void collectAsCSV(String heapName, String dest, long dominatorMinSize, int branchingFactor, int maxDepth, int maxOutbounds, long histoMinSize, long threadMinSize) throws Exception {
            logFile.delete();
            logFile.createNewFile();

            ArrayList<DomTreeObject> domRoots = collectDominatorRoots(heapName, dominatorMinSize);
            
            FileOutputStream dominatorsFOS = new FileOutputStream(dest + "/dominators.csv");    
            ArrayList<DomTreeObject> domTree = collectDomTree
            (heapName, domRoots, dominatorMinSize, branchingFactor, maxDepth);
            for (DomTreeObject obj : domTree) {
                dominatorsFOS.write(obj.toCSV().getBytes());
            }
            dominatorsFOS.close();

            FileOutputStream outboundsFOS = new FileOutputStream(dest + "/outbounds.csv");    
            ArrayList<Outbounds> outbounds = collectRootOutbounds(heapName, domRoots, maxOutbounds);
            for (Outbounds ob : outbounds) {
                outboundsFOS.write(ob.toCSV().getBytes());
            }
            outboundsFOS.close();

            // FileOutputStream histogramFOS = new FileOutputStream(dest + "/histogram.csv");
            // ArrayList <ClassHistoInfo> histogram = collectHistogram(heapName, histoMinSize);
            // for (ClassHistoInfo h : histogram) {
            //     histogramFOS.write(h.toCSV().getBytes());
            // }
            // histogramFOS.close();

            FileOutputStream threadsFOS = new FileOutputStream(dest + "/threads.csv");
            ArrayList<ThreadInfo> threads = collectThreads(heapName, threadMinSize);
            for (ThreadInfo t : threads) {
                threadsFOS.write(t.toCSV().getBytes());
            }
            threadsFOS.close();

            FileOutputStream stackTraceFOS = new FileOutputStream(dest + "/stacktrace.csv");
            ArrayList<StackFrame> stackFrames = collectStackTraces(heapName, threads);
            for (StackFrame sf : stackFrames) {
                stackTraceFOS.write(sf.toCSV().getBytes());
            }
            stackTraceFOS.close();

            FileOutputStream gcRootsFOS = new FileOutputStream(dest + "/gcroots.csv");
            ArrayList<PathToGCRootElement> gcPaths = collectPathsToGCRoots(heapName, domRoots, gcRootsFOS);
            for (PathToGCRootElement p : gcPaths) {
                gcRootsFOS.write(p.toCSV().getBytes());
            }
            gcRootsFOS.close();
    }

}
