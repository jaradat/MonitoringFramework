package com.ma.monitoringlibrary;

import android.support.annotation.Nullable;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

public class Compression {

    /**GZIPStream**/
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

    /**DeflateStream**/
    public static String DeflateCompress(@Nullable String inputString)
    {
        String str = null;
        try {
            // Encode a String into bytes
            byte[] input = inputString.getBytes("UTF-8");
            //Log.e("Compress2", "size before Compress : " + input.length + " byte");

            // Compress the bytes
            Deflater compresser = new Deflater(Deflater.BEST_SPEED);
            compresser.setInput(input);
            compresser.finish();

            byte[] output = new byte[input.length+1];
            int compressedDataLength = compresser.deflate(output);
            compresser.end();

            byte[] cnvrt = Arrays.copyOfRange(output, 0, compressedDataLength);
            str = Base64.encodeToString(cnvrt, 0);
            //str += "=";
            //Log.e("Compress2", "string after Compress : " + str);
            //Log.e("Compress2", "size after Compress : " + compressedDataLength + " byte" + "str 64 size = " + str.length());

        } catch (java.io.UnsupportedEncodingException ex) {
            // handle
        }
        return str;
    }


    public static String DeflateDecompress(String compressedText)
    {
        String outputString = null;
        try {
            byte[] output = Base64.decode(compressedText, 0);
            int compressedDataLength = output.length;

            // Decompress the bytes
            Inflater decompresser = new Inflater();
            decompresser.setInput(output, 0, compressedDataLength);
            byte[] result = new byte[80000];
            int resultLength = 0;
            resultLength = decompresser.inflate(result);
            decompresser.end();
            // Decode the bytes into a String
            outputString = new String(result, 0, resultLength, "UTF-8");
            //Log.e("Compress2", "string after deCompress : " + outputString );
            //Log.e("Compress2", "size after deCompress : " + outputString.getBytes().length + " byte");


        } catch (DataFormatException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return outputString;
    }
}
