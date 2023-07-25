package com.salesforce.cpp.heaphub.collect.collectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.salesforce.cpp.heaphub.common.HeapHubDatabaseManager;

public class CollectBase {
        // synchronous client
        static CloseableHttpClient CLIENT_SYNC = HttpClients.createDefault();
        
        static PrintWriter printWriter;
        static FileWriter fileWriter;
        // logger to write to log.txt file
        static String logFilePath = "/Users/dbarra/git/heaphub/outputs/log.txt";
        static File logFile = new File(logFilePath);
        static void log(Object o) {
            try {
                FileOutputStream fos = new FileOutputStream(logFile, true);
                fos.write((o.toString()+"\n").getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        static void log(Exception e) throws IOException {
            if (fileWriter == null) {
            fileWriter = new FileWriter(logFile, true);
            printWriter = new PrintWriter(fileWriter);
            }
            e.printStackTrace(printWriter);
        }

        static HeapHubDatabaseManager driver = HeapHubDatabaseManager.getInstance();

}
