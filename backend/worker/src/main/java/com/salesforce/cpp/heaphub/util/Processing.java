package com.salesforce.cpp.heaphub.util;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jifa.worker.Constant;

public class Processing {
    static CloseableHttpClient CLIENT_SYNC = HttpClients.createDefault();

    public static void Analyze(String fileName) throws ClientProtocolException, IOException {
		HttpPost analyze = new HttpPost(Constant.API.HEAP_DUMP_API_PREFIX + "/" + fileName + "/analyze");
		CLIENT_SYNC.execute(analyze);
    }
}
