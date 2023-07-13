/**
 * 
 */
package com.salesforce.cpp.heaphub.util;

import java.io.*;
import java.nio.charset.Charset;


/**
 * @author dvayyala
 *
 */
public class HeapDumpSplitter {
	
	public static void main(String[] args) {
        String inputHeapDumpFile = "/Users/dvayyala/Downloads/scrubbed_cs225-app2-4-ia4-14058.hprof"; // Path to the input heap dump file
        String outputDirectory = "/Users/dvayyala/Downloads/chunks/"; // Path to the output directory for chunks
        int maxChunkSize = 1024 * 1024 * 1024; // Maximum size of each chunk in bytes

        try (FileInputStream fis = new FileInputStream(inputHeapDumpFile)) {
            int chunkCounter = 0;
            long fileSize = new File(inputHeapDumpFile).length();

            while (fileSize > 0) {
            	int chunkSize = (int) Math.min(maxChunkSize, fileSize);
                byte[] buffer = new byte[chunkSize];
                int bytesRead = fis.read(buffer);

                if (bytesRead == -1) {
                    break;
                }
                String chunkFilePath = outputDirectory + "scrubbed_cs225-app2-4-ia4-14058_chunk" + chunkCounter + ".hprof";
                try (FileOutputStream fos = new FileOutputStream(chunkFilePath)) {
                	fos.write(buffer, 0, bytesRead);
                }

                chunkCounter++;
                fileSize -= bytesRead;
            }

            System.out.println("Heap dump divided into chunks successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
