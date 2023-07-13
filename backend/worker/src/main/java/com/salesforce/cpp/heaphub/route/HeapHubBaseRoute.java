/**
 * 
 */
package com.salesforce.cpp.heaphub.route;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jifa.worker.route.BaseRoute;
import org.eclipse.jifa.worker.route.MappingPrefix;
/**
 * @author dvayyala
 *
 */
@MappingPrefix("/heaphub")
public class HeapHubBaseRoute extends BaseRoute{
	
	private static List<Class<? extends HeapHubBaseRoute>> ROUTES = new ArrayList<>();

    static {
        ROUTES.add(HeapHubTestRoute.class);
    }

    public static List<Class<? extends HeapHubBaseRoute>> routes() {
        return ROUTES;
    }

}
