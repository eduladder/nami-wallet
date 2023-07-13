/**
 * 
 */
package com.salesforce.cpp.heaphub.route;

import java.util.List;

import org.eclipse.jifa.worker.route.BaseRoute;
import org.eclipse.jifa.worker.route.RouteMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;

import io.vertx.core.Future;

import io.vertx.ext.web.RoutingContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.*;

import org.json.JSONArray;

/**
 * @author dvayyala
 *
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

}
