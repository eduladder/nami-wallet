package com.salesforce.cpp.heaphub.route;

import java.util.List;

import org.eclipse.jifa.worker.Global;
import org.eclipse.jifa.worker.route.BaseRoute;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.*;

import org.json.JSONArray;

/**
 * @author dvayyala
 * test routes to test connection to db and application
 * Also can be used as example of how to create routes
 */
@SuppressWarnings("unused")
public class HeapHubTestRoute extends HeapHubBaseRoute {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(HeapHubTestRoute.class);
    
    @RouteMeta(path = "/test")
    void test(Future<String> future) {

    	future.complete("Welcome to HeapHUB!!");
    }
    
    
    @RouteMeta(path = "/testdb")
    void testDB(Future<JSONArray> future) {
    	
    	HeapHubDatabaseManager hhdm= HeapHubDatabaseManager.getInstance();
    	JSONArray resp = hhdm.executeSelect("select * from dominator_tree");
    	future.complete(resp);
    }
    
    @RouteMeta(path = "/testinsert", method = HttpMethod.GET)
    void testInsert(Future<JsonObject> future, RoutingContext context) {
    	
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

}
