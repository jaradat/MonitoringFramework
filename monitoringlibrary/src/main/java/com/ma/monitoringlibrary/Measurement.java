package com.ma.monitoringlibrary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;


import com.ma.monitoringlibrary.SettingActivity.SettingActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPInputStream;


import static android.content.Context.MODE_PRIVATE;

public class Measurement {

    private Context context;
    private SharedPreferences preferences;
    private static final int reportingRate = 10;
    private static final int queuingRate = 10;

    private static double PercentageHttp = 0.8;
    private static double PercentageDNS = 0.8;

    //initializing tools
    static public void init(Activity activity) {
        new Configuration(activity);
    }

    public static void showSettingActivity(Context context) {
        context.startActivity(new Intent(context, SettingActivity.class));
    }

    public Measurement(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(context.getString(R.string.monitoringPref), MODE_PRIVATE);


        //check tool is initializing
        if (!appIsInitialized())
            throw new RuntimeException("APP is not initialized using Measurement library : Call Measurement#init first");
    }

    private boolean appIsInitialized() {
        return preferences.getBoolean(context.getString(R.string.AppIsInit), false);
    }

    protected static void setPercentageHttp(double percentageHttp) {
        if (percentageHttp > 0 && percentageHttp <= 1)
            PercentageHttp = percentageHttp;
        else
            throw new RuntimeException("double percentageHttp must be between 1 and 0 , 1 for 100% ");

    }

    protected static void setPercentageDNS(double percentDANS) {
        if (percentDANS > 0 && percentDANS <= 1)
            PercentageDNS = percentDANS;
        else
            throw new RuntimeException("double percentDANS must be between 1 and 0 , 1 for 100%  ");

    }

    private void getPercentage()
    {
        PercentageHttp = preferences.getFloat(Type.HTTP_REQUEST + " percentage", 1);
        PercentageDNS = preferences.getFloat(Type.DNS_REQUEST + " percentage", 1);
    }

    public synchronized int start(Type type) {
        int id = 1;
        if (!preferences.getBoolean(context.getString(R.string.ToolsStatus), true))
            return -1;

        if (type == Type.HTTP_REQUEST) {
            id = preferences.getInt(context.getString(R.string.HttpIdCounter), 1);
            preferences.edit().putInt(context.getString(R.string.HttpIdCounter), id + 1).apply();
        } else if (type == Type.DNS_REQUEST) {
            id = preferences.getInt(context.getString(R.string.DnsIdCounter), 1);
            preferences.edit().putInt(context.getString(R.string.DnsIdCounter), id + 1).apply();
        }

        long currentTime = System.currentTimeMillis();
        preferences.edit().putLong("Time start :" + id + "_" + type, currentTime).apply();
        return id;
    }

    @SuppressLint("SimpleDateFormat")
    public synchronized void end(Type type, int id) {
        if (id == -1)
            return;

        long currentTime = System.currentTimeMillis();

        long startTime = preferences.getLong("Time start :" + id + "_" + type, -1);
        if (startTime == -1)
            throw new RuntimeException("id not recorded");

        long measuredTime = currentTime - startTime;


        Date currentData = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

        SharedPreferences.Editor editor = preferences.edit();

        float longLoc = preferences.getFloat("location long", 200);
        float latLoc = preferences.getFloat("location lat", 100);

        editor.putFloat("Time total :" + id + "_" + type, measuredTime);
        editor.putFloat("Location long :" + id + "_" + type, longLoc);
        editor.putFloat("Location lat :" + id + "_" + type, latLoc);
        editor.putString("Data :" + id + "_" + type, sdf.format(currentData));
        editor.apply();

        Log.e("measurements array", String.valueOf(id));

        if (id % queuingRate == 0)
            createPackage(id, type);
    }


    private void createPackage(int currentId, Type type) {
        int numberOfRecordedMeasures = preferences.getInt("numberOfRecordedMeasures", 0);
        Log.e("measurements array", "numberOfRecordedMeasures" + String.valueOf(numberOfRecordedMeasures));

        if (numberOfRecordedMeasures == 0)
            newReportingPackage(currentId, type);
        else
            updateReportingPackage(currentId, type, numberOfRecordedMeasures);
    }

    private void newReportingPackage(int currentId, Type type) {
        getPercentage();
        int numberOfItems = (int) (queuingRate * (type == Type.HTTP_REQUEST ? PercentageHttp : PercentageDNS));

        int firstId = (currentId - queuingRate) + 1;
        JSONObject object = new JSONObject();


        try {
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;

            int developerID = bundle.getInt("DeveloperID");
            String appID = bundle.getString("AppID");
            String deviceID = preferences.getString(context.getString(R.string.deviceID), "not Yet");

            object.put("developerID", developerID);
            object.put("appID", appID);
            object.put("deviceID", deviceID);


            JSONArray jsonArray = new JSONArray();
            SharedPreferences.Editor editor = preferences.edit();
            for (int i = 0; i < numberOfItems; i++) {
                JSONObject measureObject = new JSONObject();

                long locationLong = (long) (preferences.getFloat("Location long :" + (firstId + i) + "_" + type, 200) * Math.pow(10.0, 15.0));
                long locationLat = (long) (preferences.getFloat("Location lat :" + (firstId + i) + "_" + type, 100) * Math.pow(10.0, 15.0));


                measureObject.put("Time total", preferences.getFloat("Time total :" + (firstId + i) + "_" + type, -1));
                measureObject.put("Location long", locationLong);
                measureObject.put("Location lat", locationLat);
                measureObject.put("date", preferences.getString("Data :" + (firstId + i) + "_" + type, ""));
                measureObject.put("type", type);

                jsonArray.put(measureObject);
                // clone preferences
                editor.remove("Time total :" + (firstId + i) + "_" + type);
                editor.remove("Location long :" + (firstId + i) + "_" + type);
                editor.remove("Location lat :" + (firstId + i) + "_" + type);
                editor.remove("Data :" + (firstId + i) + "_" + type);

            }

            editor.apply();

            object.put("measurements array", jsonArray);

            Log.e("JSONRecord", object.toString());
            //update number Of Recorded Measures
            preferences.edit().putInt("numberOfRecordedMeasures", numberOfItems).apply();
            preferences.edit().putString("measurementsJsonString", object.toString()).apply();


        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateReportingPackage(int currentId, Type type, int numberOfRecordedMeasures) {

        String JsonString = preferences.getString("measurementsJsonString", null);
        if (JsonString == null) {
            preferences.edit().putInt("numberOfRecordedMeasures", 0).apply();
            return;
        }


        try {
            getPercentage();
            int numberOfItem = (int) (queuingRate * (type == Type.HTTP_REQUEST ? PercentageHttp : PercentageDNS));
            int firstId = (currentId - queuingRate) + 1;

            JSONObject packageObject = new JSONObject(JsonString);
            JSONArray jsonArray = packageObject.getJSONArray("measurements array");

            for (int i = 0; i < numberOfItem; i++) {
                JSONObject measureObject = new JSONObject();

                long locationLong = (long) (preferences.getFloat("Location long :" + (firstId + i) + "_" + type, 200) * Math.pow(10.0, 15.0));
                long locationLat = (long) (preferences.getFloat("Location lat :" + (firstId + i) + "_" + type, 100) * Math.pow(10.0, 15.0));


                measureObject.put("Time total", preferences.getFloat("Time total :" + (firstId + i) + "_" + type, -1));
                measureObject.put("Location long", locationLong);
                measureObject.put("Location lat", locationLat);
                measureObject.put("date", preferences.getString("Data :" + (firstId + i) + "_" + type, ""));
                measureObject.put("type", type);

                jsonArray.put(measureObject);
            }

            packageObject.put("measurements array", jsonArray);
            preferences.edit().putString("measurementsJsonString", packageObject.toString()).apply();

            int newNumberOfRecordedMeasures = numberOfRecordedMeasures + numberOfItem;
            if (newNumberOfRecordedMeasures >= reportingRate) {
                try {
                    Log.e("origin", String.valueOf(packageObject.toString()));

                    //Log.e("compression","packageObject.toString() size: " +  packageObject.toString().length());

                    String compressedPackage = Compression.DeflateCompress(packageObject.toString());
                    //packageObject.toString().replace("\"","\\\"");// w/o compression
                    //   String encryptPackage = new Encryption(String.valueOf(System.currentTimeMillis())).Encrypt(compressedPackage);

                    Encryption encryption=new Encryption();
                    String encryptedPackage = encryption.Encrypt(compressedPackage);
                    SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.monitoringPref), MODE_PRIVATE);

                    JSONObject data = new JSONObject();
                    data.put("encryptedPackage", compressedPackage);//encryptedPackage));
                    data.put("SecretKey", encryption.getEncryptedSecretKey());
                    data.put("token", prefs.getString("token", ""));

                    /*TODO Send encryptPackage */
                    //String x =  "{\"encryptedPackage\":\"H4sIAAAAAAAAAMWXT2sDIRDFv8oy5y3VGV11zz20pYce9lBoe7CJLQv7j40thJDv3k1PTRrIrU9QUNTfPEZ8uqN1+krdOKX57oZqim+rp3sjhkrqU9x8zqlPQ94UcZ7jlurnHTVtn4o85thRzexLWseclpWstH+51nyoy+q8nQ6jt03zuPQexlXM7TgU3Th8UH2l1XH5PSPmMxP25TFZB0GRvQOR2WgQWRQqz6ZilGaD0iy2QpGFTxnn47iw7+XQ\/uTZORBZOKA0BwUiswiK7GCafYUiVx51tsXAbk+UP4sNKK9SKM2sLIrMMLJGuSRr1MuAYf4sFkXWAUVm1jAy6k3CBkauYP5s\/y3PryXFaTr9aS9f8HaVfkad53flJC6tBGsU7b8BFNOYfaIPAAA=\",\"token\":\"fOnZft7JvS4:APA91bFWy_uEym2XxgT25v_o57SSawXcB_eQOhVWjGmTk4PpyrvdaFaN_QKXjDE2fq6yLYHtrRDI67acu-haVIHoi85lk2RnL31jiLbtIErGufuQ9xsimfecrC6kB5t8PMP8mYTnOrjl\",\"SecretKey\":\"qT040yHxofFLqbSOWR9kwMDmKiXf6P42uqW+2Y8DRqWrz+CowLgt3VPdMy40IsPaCkJv5kfRzJFC\nP\/OyNU+7NmDpfMKdx00fJnwk8ttKVFrsDsFx5fdP9N35UJswmko\/xVKnAZlmP23LjVVRyYIc2hDj\nthhH9hHGL6gg4tYfBHA=\n\"}";
                    String data_str  = data.toString().replace("\"","\\\"");
                    //data_str = data_str.replace("{", "\"{");
                    //data_str = data_str.replace("}", "}\"");
                    new UploadTimeMeasurementsTask().execute(data_str);

                    Log.e("compression",compressedPackage );//+ " with size: " +  Compression.Compress(compressedPackage).length());
                    Log.e("encryption", encryptedPackage );//+ " with size: " +  encryptedPackage);
                    Log.e("SecretKey", encryption.getEncryptedSecretKey());
                    Log.e("all data", data_str);



                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else
                preferences.edit().putInt("numberOfRecordedMeasures", newNumberOfRecordedMeasures).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public enum Type {
        HTTP_REQUEST,
        DNS_REQUEST,
        MEM_ALLOC
    }

    public class UploadTimeMeasurementsTask extends AsyncTask<String,String,String>
    {
        @Override
        protected void onPreExecute()
        {
        }

        @Override
        protected String doInBackground(String... params)
        {
            String response = uploadTimeMeasurements(params[0]);

            Log.e("uploadTimeMeasurements", response);
            return response;
        }

        @Override
        protected void onPostExecute(String result)
        {
            preferences.edit().putInt("numberOfRecordedMeasures",0).apply();
            super.onPostExecute(result);
        }

        public String uploadTimeMeasurements(String data)
        {
            URL url = null;
            StringBuilder builder = new StringBuilder();
            HttpURLConnection connection = null;
            try {
                // create url
                url = new URL("http://framework1-001-site1.htempurl.com/api/monitoringtool/UploadTimeMeasurements");
                // open a connection
                connection = (HttpURLConnection) url.openConnection();

                connection.setDoInput(true); // to get request only
                connection.setDoOutput(true); // upload a request body
                connection.setUseCaches(false);
                connection.setRequestMethod("POST"); // request method post
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept-Encoding","gzip");
                connection.setRequestProperty("Content-Language", "en-US");
                connection.setConnectTimeout(30000); // connection time out
                connection.setReadTimeout(30000); // read time out

                //long t = System.currentTimeMillis();
                //HttpResponse response = (HttpResponse) httpclient.execute(httpPostRequest);
                //Log.i(TAG, "HTTPResponse received in [" + (System.currentTimeMillis()-t) + "ms]");


                // Send request
                String json = "{\"data\":\"" + data + "\"}";
                connection.setRequestProperty("Content-Length","" + Integer.toString(json.getBytes().length));
                OutputStream outStream = new BufferedOutputStream(connection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
                writer.write(json);
                writer.flush();
                writer.close();
                outStream.close();

                // get response
                InputStream inStream;
                if (connection.getContentEncoding()!= null &&connection.getContentEncoding().equalsIgnoreCase("gzip"))
                {
                    inStream = new GZIPInputStream(connection.getInputStream());
                } else {
                    //in = new BufferedInputStream(connection.getInputStream());
                    inStream = connection.getInputStream(); // input stream of connection to get data
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream)); // reader for reading data from stream
                String line;
                while((line = reader.readLine()) != null)
                {
                    builder.append(line);
                }
                int responseCode = connection.getResponseCode();
                reader.close();
                if(responseCode == HttpURLConnection.HTTP_OK)
                {
                    return builder.toString();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return builder.toString();
        }

    }
}
