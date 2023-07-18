package com.salesforce.cpp.heaphub.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import io.vertx.core.json.JsonObject;

public class Response {

    private int statusCode;

    private String statusPhrase;

    private JsonObject body;

	String errorsFilePath = "/Users/dbarra/git/heaphub/outputs/errors.txt";

    File errorsFile = new File(errorsFilePath);

    // write text to errorsFile
    void log(Object o) {
        try {
            FileOutputStream fos = new FileOutputStream(errorsFile, true);
            fos.write((o.toString()+"\n").getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Response(HttpResponse res) throws ParseException, IOException {
        StatusLine statusLine = res.getStatusLine();
		statusCode = statusLine.getStatusCode();
        statusPhrase = statusLine.getReasonPhrase();
		if (statusCode >= 300) {
			log("Error Code: " + statusCode + " " + statusLine.getReasonPhrase());
			String responseBodyString = EntityUtils.toString(res.getEntity());
			log(responseBodyString);
			return;
		}
		HttpEntity resEntity = res.getEntity();
	    if (resEntity == null) {
			log("Code: " + statusCode + " " + statusLine.getReasonPhrase());
			log("Response contains no content");
			return;
	    }
		// log("Code: " + statusCode + " " + statusLine.getReasonPhrase());
	    String responseBodyString = EntityUtils.toString(resEntity);
//		System.out.println(responseBodyString == "");
	    // System.out.println(responseBodyString);
	    body = new JsonObject(responseBodyString);
    }

    public int getStatusCode() {
		return statusCode;
	}

	public String getStatusPhrase() {
		return statusPhrase;
	}

	public JsonObject getBody(){
		return body;
	}
}
