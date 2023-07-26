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

public class CollectOutbounds extends CollectBase {

    private String heapName;
    private int heapId;
    private long createdAt;
    private ArrayList<DomTreeObject> roots;
    private int maxOutbounds;

    public CollectOutbounds(String heapName, int heapId, long createdAt, ArrayList<DomTreeObject> roots, int maxOutbounds) {
        this.heapName = heapName;
        this.createdAt = createdAt;
        this.roots = roots;
        this.maxOutbounds = maxOutbounds;
        this.heapId = heapId;
    }

    private ArrayList<Outbounds> collectSingleRootOutbounds( DomTreeObject root) {
        try {
            int i = 1;
            int cnt = 0;
            int id = root.getObjectId();
            boolean loop = true;
            ArrayList<Outbounds> arr = new ArrayList<Outbounds>(32);
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
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Outbounds> getOutbounds(int objectId, int page, int pageSize){
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
                ArrayList<Outbounds> arr = new ArrayList<Outbounds>(32);
                // TODO: update with proper return format
                for (int i = 0; i < jsonArray.size(); i++) {
                    // print now until we decide how to store data
                    JsonObject curr = jsonArray.getJsonObject(i);
                    arr.add(new Outbounds(curr, objectId, heapId, createdAt));
                    // produce to next object in array
                }
                return arr;
            }
        } catch (Exception e) {
            // TODO
            return null;
        }
    }

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

    public void collectAndUpload() throws IOException {
        ArrayList<Outbounds> outbounds = collectRootOutbounds();
        StringBuilder sb = new StringBuilder(Outbounds.uploadSQLStatement());
        int cnt = 0;
        for (Outbounds obj : outbounds) {
            if (cnt > 0) {
                sb.append(", ");
            }
            sb.append(obj.getSQLValues());
            cnt++;
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
