package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Stack;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;

import com.salesforce.cpp.heaphub.collect.models.DomTreeObject;
import com.salesforce.cpp.heaphub.collect.models.GCRootPath;
import com.salesforce.cpp.heaphub.collect.models.GCRootPath.PathToGCRootElement;
import com.salesforce.cpp.heaphub.collect.models.Outbounds;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class CollectGCRoot extends CollectBase {
    
    private String heapName;
    private int heapId;
    private long createdAt; 
    private ArrayList<DomTreeObject> roots;
    private int maxDepth;
    private int branchingFactor;

    public CollectGCRoot(String heapName, int heapId, long createdAt, ArrayList<DomTreeObject> roots, int maxDepth, int branchingFactor) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.roots = roots;
        this.maxDepth = maxDepth;
        this.branchingFactor = branchingFactor;
    }
    
    
    public ArrayList<PathToGCRootElement> collectPathsToGCRoots() {
        try {
            ArrayList<PathToGCRootElement> acc = new ArrayList<PathToGCRootElement>();
            for (DomTreeObject root : roots) {
                ArrayList<PathToGCRootElement> path = collectGCPath( root);
                acc.addAll(path);
            }
            return acc;
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    
    public ArrayList<PathToGCRootElement> collectGCPath(DomTreeObject root) throws IOException {
            ArrayList<PathToGCRootElement> acc = new ArrayList<PathToGCRootElement>();
            int i = 0;
            GCRootPath curr = getPathToGCRoots(root.getObjectId(), 0, 10);
            acc.addAll(curr.getPath());
            while (curr.hasMore() && i < branchingFactor) {
                curr = getPathToGCRoots(root.getObjectId(), 10 * i, 10);
                acc.addAll(curr.getPath());
                i++;
            }
            log(acc.size());
            return acc;
    }

    public GCRootPath getPathToGCRoots(int originId, int skip, int count)
    {
    try {
            URI uri = new URIBuilder(Constant.API.HEAP_DUMP_API_PREFIX + "/" + heapName + "/pathToGCRoots")
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
                ArrayList<PathToGCRootElement> pathList = collectTree(tree, originId);
                GCRootPath path = new GCRootPath(pathList, resJSON.getBoolean("hasMore"));
                return path;
            }
        } catch (Exception e) {
            // TODO
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log(sw.toString());
            return null;
        }
    }

    public ArrayList<PathToGCRootElement> collectTree(JsonObject src, int originId) throws Exception {
        class StackObjInfo {
            JsonObject node;
            int parentId;
            int depth;
            StackObjInfo(JsonObject node, int parentId, int depth) {
                this.node = node;
                this.parentId = parentId;
                this.depth = depth;
            }
        }
    
        Stack<StackObjInfo> stack = new Stack<StackObjInfo>();
        stack.push(new StackObjInfo(src, -1, 0));
        
        ArrayList<PathToGCRootElement> output = new ArrayList<PathToGCRootElement>();        
    
        while (!stack.isEmpty()) {
            StackObjInfo curr = stack.pop();
            output.add(new PathToGCRootElement(curr.node, curr.parentId, originId, heapId, createdAt));
            if (curr.depth < maxDepth) {
                JsonArray children = curr.node.getJsonArray("children");
                for (int i = 0; i < Math.min(children.size(), branchingFactor); i++) {
                    stack.push(new StackObjInfo(children.getJsonObject(i), curr.node.getInteger("objectId"), curr.depth + 1));
                }
            }
        }
        return output;
    }

    public void collectAndUpload() throws IOException {
        ArrayList<PathToGCRootElement> gcPaths = collectPathsToGCRoots();
        StringBuilder sb = new StringBuilder(PathToGCRootElement.uploadSQLStatement());
        int cnt = 0;
        for (PathToGCRootElement obj : gcPaths) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(obj.getSQLValues());
            cnt++;
            if (cnt == 100) {
                sb.append(";");
                driver.executeUpdate(sb.toString());
                sb = new StringBuilder(PathToGCRootElement.uploadSQLStatement());
                cnt = 0;
            }
        }
        if (cnt != 0) {
            sb.append(";");
            driver.executeUpdate(sb.toString());
        }
    }
}
