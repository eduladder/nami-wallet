package com.salesforce.cpp.heaphub.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import io.vertx.core.json.JsonObject;


/**
 * A util class to parse a response from org.apache.http.impl.client.HttpClients and store the body as a io.vertx.core.json.JsonObject
 */
public class Response {

    private int statusCode; // the status code of the resposne

    private String statusPhrase; // the status phrase of the response 

    private JsonObject body; // the body of the response


	String logFilePath = "/Users/dbarra/git/heaphub/outputs/log.txt";

    File logFile = new File(logFilePath);

    // log function
    void log(Object o) {
        try {
            FileOutputStream fos = new FileOutputStream(logFile, true);
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
		// if failure, log error and return;
		if (statusCode >= 300) {
			log("Error Code: " + statusCode + " " + statusLine.getReasonPhrase());
			String responseBodyString = EntityUtils.toString(res.getEntity());
			log(responseBodyString);
			return;
		}
		HttpEntity resEntity = res.getEntity();
		// if not a failure but response has not body, log as such and return
	    if (resEntity == null) {
			log("Code: " + statusCode + " " + statusLine.getReasonPhrase());
			log("Response contains no content");
			return;
	    }
		// convert body of response to io.vertx.core.json.JsonObject
	    String responseBodyString = EntityUtils.toString(resEntity);
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
