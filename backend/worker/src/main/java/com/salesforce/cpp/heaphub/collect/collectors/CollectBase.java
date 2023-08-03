package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;

/* 
* a base class to define static resources shared by all Collect classes.
*/
public class CollectBase {
        // synchronous client
        static CloseableHttpClient CLIENT_SYNC = HttpClients.createDefault();
        
        static StringWriter sw;
        static PrintWriter printWriter;
        // file path for log 
        static String logFilePath = "/Users/dbarra/git/heaphub/outputs/log.txt";
        static File logFile = new File(logFilePath);
        // log function to allow for logging once project is built
        static void log(Object o) {
            try {
                FileOutputStream fos = new FileOutputStream(logFile, true);
                fos.write((o.toString()+"\n").getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // overload log function to improve logging of exceptions
        static void log(Exception e) throws IOException {
            if (sw == null) {
            StringWriter sw = new StringWriter();
            printWriter = new PrintWriter(sw);
            }
            e.printStackTrace(printWriter);
            log(sw.toString());
            sw.flush();
        }

        static HeapHubDatabaseManager driver = HeapHubDatabaseManager.getInstance();

}
