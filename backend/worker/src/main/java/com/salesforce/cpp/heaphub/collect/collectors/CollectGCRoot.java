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
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Collector class to collect the paths to garbage collection roots for each colelcted root in the heap dump's dominator tree. Upload the results to SQL. Since this is a tree data structure, the data collected is restricted by the passed in maximum depth and branching factor.
 */
public class CollectGCRoot extends CollectBase {
    
    private String heapName;
    private int heapId;
    private long createdAt; 
    private ArrayList<DomTreeObject> roots;
    private int maxDepth;
    private int branchingFactor;


    /**
     * Constructor for the class.
     * @param heapName generated name of the heap
     * @param heapId primary key id of heap in SQL database
     * @param createdAt time when analysis is being conducted
     * @param roots list of all the roots in the heap dump's dominator tree
     * @param maxDepth maximum depth of the tree to traverse
     * @param branchingFactor maximum branching factor of the tree to traverse
     */
    public CollectGCRoot(String heapName, int heapId, long createdAt, ArrayList<DomTreeObject> roots, int maxDepth, int branchingFactor) {
        this.heapName = heapName;
        this.heapId = heapId;
        this.createdAt = createdAt;
        this.roots = roots;
        this.maxDepth = maxDepth;
        this.branchingFactor = branchingFactor;
    }
    
    /***
     * Collect all the paths to the garbage collection roots for each root in the heap dump's dominator tree.
     * @return ArrayList<PathToGCRootElement> which contains all the paths to the garbage collection roots
      */
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
    
    /***
     * Collect the path(s) to the GC roots for a given root in the heap dump's dominator tree.
     * @param root DomTreeObject which represents a root node in the dominator tree
     * @return ArrayList<PathToGCRootElement> which contains all the paths to the GC roots for a given root in the heap dump's dominator tree
     */
    public ArrayList<PathToGCRootElement> collectGCPath(DomTreeObject root) throws IOException {
            ArrayList<PathToGCRootElement> acc = new ArrayList<PathToGCRootElement>();
            int i = 0;
            GCRootPath curr = getPathToGCRoots(root.getObjectId(), 0, 10);
            acc.addAll(curr.getPath());
            // if the branching factor is reached or there is no more to collect, stop collecting paths
            while (curr.hasMore() && i < branchingFactor) {
                curr = getPathToGCRoots(root.getObjectId(), 10 * i, 10);
                acc.addAll(curr.getPath());
                i++;
            }
            log(acc.size());
            return acc;
    }


    /**
      * Make a call to the JIFA backend to get the path to the GC roots for a given objectId and convert the result to a GCRootPath object.
      * @param originId objectId of the root to start the path from
      * @param skip skip this number of objects in the path (note it is not exactly clear how this works in the JIFA backend)
       * @param count number of objects to include in the path (note it is not exactly clear how this works in the JIFA backend)
      * @return GCRootPath object containing the path to the GC roots
      */
    public GCRootPath getPathToGCRoots(int originId, int skip, int count)
    {
        try {
            // make request to JIFA API
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
                // convert result to desired dataformat
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

    /**
     * Helper function to process returned Json from request to JIFA API and extract the needed information.
     * @param src JsonObject to process
     * @param originId objectId of the root to start the path from
     * @return
     * @throws Exception
     */
    public ArrayList<PathToGCRootElement> collectTree(JsonObject src, int originId) throws Exception {
        // local stack class to allow for non recursive DFS traversal
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
        
        // initialize stack with root of tree
        Stack<StackObjInfo> stack = new Stack<StackObjInfo>();
        // NOTE: THE SRC APPEARS TO ALWAYS HAVE THE ROOT AS THE FIRST ELEMENT IN THE TREE AND WE ASSUME THAT THIS IS THE PROPER PARENT OF THE NEXT ELEMENT IN THE JSON TREE. IT IS UNCLEAR IF THIS IS TRUE IN ALL CASES.
        // TODO: verify with frontend representation of path to GC root to see if this assumption makes sense
        stack.push(new StackObjInfo(src, -1, 0));
        
        ArrayList<PathToGCRootElement> output = new ArrayList<PathToGCRootElement>();        
    
        while (!stack.isEmpty()) {
            StackObjInfo curr = stack.pop();
            output.add(new PathToGCRootElement(curr.node, curr.parentId, originId, heapId, createdAt));
            // limit collection to max depth
            if (curr.depth < maxDepth) {
                JsonArray children = curr.node.getJsonArray("children");
                // add children of current node to stack but limit by branching factor
                for (int i = 0; i < Math.min(children.size(), branchingFactor); i++) {
                    stack.push(new StackObjInfo(children.getJsonObject(i), curr.node.getInteger("objectId"), curr.depth + 1));
                }
            }
        }
        return output;
    }


    /**
     * Collect all the paths to GC roots for the heap dump and upload the results to SQL.
      * @throws IOException
     */
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
