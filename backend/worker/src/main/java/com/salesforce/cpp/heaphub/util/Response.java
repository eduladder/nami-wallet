package com.salesforce.cpp.heaphub.util;

import java.io.IOException;

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

    public Response(HttpResponse res) throws ParseException, IOException {
        StatusLine statusLine = res.getStatusLine();
		statusCode = statusLine.getStatusCode();
        statusPhrase = statusLine.getReasonPhrase();
		if (statusCode >= 300) {
			System.out.println("Error Code: " + statusCode + " " + statusLine.getReasonPhrase());
		String responseBodyString = EntityUtils.toString(res.getEntity());
		System.out.println(responseBodyString);
			return;
		}
		HttpEntity resEntity = res.getEntity();
	    if (resEntity == null) {
			System.out.println("Code: " + statusCode + " " + statusLine.getReasonPhrase());
			System.out.println("Response contains no content");
			return;
	    }
		System.out.println("Code: " + statusCode + " " + statusLine.getReasonPhrase());
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
