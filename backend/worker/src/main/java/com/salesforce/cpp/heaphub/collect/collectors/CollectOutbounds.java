package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jifa.worker.Constant;

import com.salesforce.cpp.heaphub.collect.models.DomTreeObject;
import com.salesforce.cpp.heaphub.collect.models.Outbounds;
import com.salesforce.cpp.heaphub.util.Response;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


/**
 * Collector class to collect all outgoing references for each roots in a heap dump's dominator tree. Upload the results to SQL. Since this is a tree data structure, the data collected is restricted by the passed in maximum depth and branching factor.
 */
public class CollectOutbounds extends CollectBase {

    private String heapName;
    private int heapId;
    private long createdAt;
    private ArrayList<DomTreeObject> roots;
    private int maxOutbounds;

    /***
     * Constructor for CollectOutbounds
     * @param heapName generated name of the heap
     * @param heapId primary key id of heap in SQL database
      * @param createdAt time when analysis is being conducted
     * @param roots collect all objects with a retained size > minSize
     * @param maxOutbounds max number of outbounds to collect to limit collection of outbounds for a given root
     */
    public CollectOutbounds(String heapName, int heapId, long createdAt, ArrayList<DomTreeObject> roots, int maxOutbounds) {
        this.heapName = heapName;
        this.createdAt = createdAt;
        this.roots = roots;
        this.maxOutbounds = maxOutbounds;
        this.heapId = heapId;
    }


    /***
     * Collect all outgoing references for a given root
      * @param root DomTreeObject which contains the root to collect outgoing references for
     * @return ArrayList<Outbounds> which contains all the outgoing references for a given root
     * @throws IOException
     */
    private ArrayList<Outbounds> collectSingleRootOutbounds(DomTreeObject root) throws IOException {
        try {
            int i = 1;
            int cnt = 0;
            int id = root.getObjectId();
            boolean loop = true;
            ArrayList<Outbounds> arr = new ArrayList<Outbounds>(32);
            // collect all object until maxOutbounds is reached
            while(loop) {
                ArrayList<Outbounds> obs = getOutbounds(id, i, 32);
                if (obs == null || obs.size() == 0) {
                    break;
                }
                for (Outbounds ob : obs) {
                    if (cnt >= maxOutbounds) {
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
            log(e);
            return null;
        }
    }

     /***
     * Collect all outgoing references for a given root
     * @param objectId object id of the root to collect outgoing references for
     * @param page page number to collect outgoing references for
     * @param pageSize page size to collect outgoing references for
      * @return ArrayList<Outbounds> which contains all the outgoing references for a given root
     * @throws IOException
     */
    public ArrayList<Outbounds> getOutbounds(int objectId, int page, int pageSize) throws IOException{
        try {
            // call JIFA API
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
                // parse response and covert to array list of outbounds
                JsonObject resJSON = res.getBody();
                if (resJSON == null) {
                    // TODO
                    return null;
                }
                JsonArray jsonArray = resJSON.getJsonArray("data");
                if (jsonArray == null) {
                    log("resJSON.data is null");
                    return null;
                }
                ArrayList<Outbounds> arr = new ArrayList<Outbounds>(32);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new Outbounds(curr, objectId, heapId, createdAt));
                }
                return arr;
            }
        } catch (Exception e) {
            log(e);
            return null;
        }
    }

     /***
     * Collect all outgoing references for all roots
     * @return ArrayList<Outbounds> which contains all the outgoing references for all roots
     * @throws IOException
     */
    public ArrayList<Outbounds> collectRootOutbounds() {
        try {
            ArrayList<Outbounds> acc = new ArrayList<Outbounds>();
            for (DomTreeObject root : roots) {
                ArrayList<Outbounds> stacktrace = collectSingleRootOutbounds(root);
                acc.addAll(stacktrace);
            }
            return acc;
        } catch (Exception e) {
            return null;
        }
    }

     /***
     * Collect all outgoing references for all roots and upload to SQL
     * @throws IOException
     */
    public void collectAndUpload() throws IOException {
        ArrayList<Outbounds> outbounds = collectRootOutbounds();
        StringBuilder sb = new StringBuilder(Outbounds.uploadSQLStatement());
        int cnt = 0;
        // use batch insert to upload to db - use batch size of 100
        for (Outbounds obj : outbounds) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(obj.getSQLValues());
            cnt++;
            // set batch size to 100
            if (cnt == 100) {
                sb.append(";");
                driver.executeUpdate(sb.toString());
                sb = new StringBuilder(Outbounds.uploadSQLStatement());
                cnt = 0;
            }
        }
        if (cnt != 0) {
            sb.append(";");
            driver.executeUpdate(sb.toString());
        }
    }

    
}
