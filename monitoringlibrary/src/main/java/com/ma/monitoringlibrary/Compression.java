package com.ma.monitoringlibrary;

import android.support.annotation.Nullable;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Compression {

    public static String Compress(@Nullable String data) throws IOException {
        try {
            // Create an output stream, and a gzip stream to wrap over.
            ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
            GZIPOutputStream gzip = new GZIPOutputStream(bos);

            // Compress the input string
            gzip.write(data.getBytes());
            gzip.close();
            byte[] compressed = bos.toByteArray();
            bos.close();

            // Convert to base64
           // compressed = Base64.decode(compressed,Base64.DEFAULT);
            compressed=  org.apache.commons.codec.binary.Base64.encodeBase64(compressed);
           // return URLEncoder.encode(new String(compressed),"UTF-8");


            // return the newly created string
            return new String(compressed);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String Decompress(String compressedText) throws IOException {
        // get the bytes for the compressed string
        byte[] compressed = compressedText.getBytes("UTF8");

        // convert the bytes from base64 to normal string
        compressed = Base64.decode(compressed,Base64.DEFAULT);

        // decode.
        final int BUFFER_SIZE = 32;

        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        StringBuilder string = new StringBuilder();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = gis.read(data)) != -1) {
            string.append(new String(data, 0, bytesRead));
        }
        gis.close();
        is.close();
        return string.toString();
    }

}
