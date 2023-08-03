package com.salesforce.cpp.heaphub.route;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jifa.worker.Constant;
import org.eclipse.jifa.worker.Global;
import org.eclipse.jifa.worker.route.BaseRoute;
import org.eclipse.jifa.worker.route.MappingPrefix;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * @author dvayyala
 * a base route for all routes created as part of heaphub extension to JIFA application
 */
@MappingPrefix("/heaphub/:file")
public class HeapHubBaseRoute extends BaseRoute{
    // location of log file to allow for deleting and rebuilding log file whenever route is hit
	static String logFilePath = "/Users/dbarra/git/heaphub/outputs/log.txt";
	static File logFile = new File(logFilePath);
	private static List<Class<? extends HeapHubBaseRoute>> ROUTES = new ArrayList<>();

    static {
    	// add routes here
        ROUTES.add(HeapHubTestRoute.class);
        ROUTES.add(HeapHubCollectRoutes.class);
    }

    public static List<Class<? extends HeapHubBaseRoute>> routes() {
        return ROUTES;
    }
    
    static WebClient CLIENT = WebClient.create(Vertx.vertx());

    static CloseableHttpClient CLIENT_SYNC = HttpClients.createDefault();

    static String TEST_HEAP_DUMP_FILENAME;

    public static String uri(String uri) {
        return Global.stringConfig(Constant.ConfigKey.API_PREFIX) + uri;
    } 

}
