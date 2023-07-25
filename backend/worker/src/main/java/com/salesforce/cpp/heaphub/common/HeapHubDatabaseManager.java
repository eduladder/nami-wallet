/**
 * 
 */
package com.salesforce.cpp.heaphub.common;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HeapHubDatabaseManager {

    private static HeapHubDatabaseManager instance;
    private Connection connection;
    
    private final static String url = "jdbc:postgresql://ec2-34-235-198-25.compute-1.amazonaws.com:5432/d16lt7hto5cdcb?currentSchema=heaphub";
    private final static String user = "sdldnzqbmrafeu";
    private final static String password = "48cb03b95211bd6c372e0c7a7a6cf6b37d09a3436271200d793a47755a86e65e";

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

    private HeapHubDatabaseManager() {
    	try {
    			connection = DriverManager.getConnection(url, user, password);
            } catch (SQLException e) {
            	e.printStackTrace();;
            }
    }

    public static HeapHubDatabaseManager getInstance() {
        if (instance == null) {
            instance = new HeapHubDatabaseManager();
            
        }
        return instance;
    }

    public JSONArray executeSelect(String sql) {
    	
    	JSONArray result = null;
    	try {
    		PreparedStatement preparedStatement = connection.prepareStatement(sql);
    		ResultSet rs = preparedStatement.executeQuery();
    		result = converResultSetToJson(rs);
    	} catch (SQLException e) {
        	e.printStackTrace();
        }
    	
    	return result;
    }

	public void executeUpdate(String sql) throws IOException {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			log("sql: " + sql);
			log(e.getMessage());
		}
	}
    
    private JSONArray converResultSetToJson(ResultSet rs) throws SQLException{
    	
    	ResultSetMetaData md = rs.getMetaData();
    	int numCols = md.getColumnCount();
    	List<String> colNames = IntStream.range(0, numCols)
    	  .mapToObj(i -> {
    	      try {
    	          return md.getColumnName(i + 1);
    	      } catch (SQLException e) {
    	          e.printStackTrace();
    	          return "?";
    	      }
    	  })
    	  .collect(Collectors.toList());

    	JSONArray result = new JSONArray();
    	while (rs.next()) {
    	    JSONObject row = new JSONObject();
    	    colNames.forEach(cn -> {
    	        try {
    	            row.put(cn, rs.getObject(cn));
    	        } catch (JSONException | SQLException e) {
    	            e.printStackTrace();
    	        }
    	    });
    	    result.put(row);
    	}
    	return result;
    }
}
