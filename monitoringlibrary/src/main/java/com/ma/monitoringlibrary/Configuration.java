package com.ma.monitoringlibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

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
import java.util.Date;
import java.util.UUID;
import java.util.zip.GZIPInputStream;


import static android.content.Context.MODE_PRIVATE;

class Configuration {

    private Activity activity;
    private SharedPreferences prefs;

    Configuration(Activity activity) {
        this.activity = activity;

        checkFirstLaunched();
    }


    @SuppressLint("MissingPermission")
    private void checkFirstLaunched() {

        prefs = activity.getApplicationContext().getSharedPreferences(activity.getString(R.string.monitoringPref), MODE_PRIVATE);

        if (!prefs.contains(activity.getString(R.string.AppIsInit))) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(activity.getString(R.string.AppIsInit), true);
            //editor.putInt(activity.getString(R.string.IdCounter),0);

            //first status set
            // true for enabled
            editor.putBoolean(activity.getString(R.string.ToolsStatus), true);
            editor.putBoolean(activity.getString(R.string.GpsStatus), true);

            editor.apply();
            // write location for first time
            writeLocation();
            // register device
            collectStatistics();

        } else {

            // write Location if location in enabled from user
            if (prefs.getBoolean(activity.getString(R.string.GpsStatus), false))
                writeLocation();


        }

    }

    @SuppressLint("MissingPermission")
    private void writeLocation() {

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    SharedPreferences.Editor editor = prefs.edit();

                    editor.putFloat("location lat", (float) location.getLatitude());
                    editor.putFloat("location long", (float) location.getLongitude());
                    editor.apply();

                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                    Log.d("onStatusChanged", provider);
                }

                public void onProviderEnabled(String provider) {
                    Log.d("onProviderEnabled", provider);
                }

                public void onProviderDisabled(String provider) {
                    Log.d("onProviderDisabled", provider);
                    checkDeviceLocationIsOn();
                }
            };

            assert locationManager != null;
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
        }

    }

    private boolean checkSelfPermission(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void checkDeviceLocationIsOn() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //this is the key ingredient

        Task<LocationSettingsResponse> result =
                LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build());
        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    // when exception catch the provider(GPS) is disable
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    // All location settings are satisfied.
                    writeLocation();

                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                // i=100 for request code onActivityResult() (is redundant now)
                                resolvable.startResolutionForResult(activity, 100);
                            } catch (IntentSender.SendIntentException | ClassCastException e) {
                                e.printStackTrace();
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            }
        });
    }

    @SuppressLint("HardwareIds")
    private void collectStatistics() {

        int apiVersion = Build.VERSION.SDK_INT;
        String buildRelease = Build.VERSION.RELEASE;
        String osVersion = System.getProperty("os.version");
        String device = Build.DEVICE;
        String model = Build.MODEL;
        String product = Build.PRODUCT;

        TelephonyManager telephonyManager = ((TelephonyManager)
                activity.getSystemService(Context.TELEPHONY_SERVICE));
        assert telephonyManager != null;
        String operatorName = telephonyManager.getNetworkOperatorName();
        String SIMOperatorName = telephonyManager.getSimOperatorName();
        String serialNumber = android.provider.Settings.Secure.getString
                (activity.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        if("9774d56d682e549c".equals(serialNumber) || serialNumber == null)
        {
            serialNumber = UUID.randomUUID().toString();
        }
        // save ANDROID_ID
        prefs.edit().putString(activity.getString(R.string.deviceID), serialNumber).apply();

        long locationLong = (long) (prefs.getFloat("Location long :" , 200) * Math.pow(10.0, 15.0));
        long locationLat = (long) (prefs.getFloat("Location lat :" , 100) * Math.pow(10.0, 15.0));


        JSONObject statisticsObject = new JSONObject();
        try {
            ApplicationInfo app = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;

            int developerID = bundle.getInt("DeveloperID");
            String appID = bundle.getString("AppID");

            prefs.edit().putInt("developerID",developerID).apply();
            prefs.edit().putString("appID",appID).apply();


            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            String currentDateTime = sdf.format(new Date());
            statisticsObject.put("timestamp", currentDateTime);
            statisticsObject.put("developerID", developerID);
            statisticsObject.put("appID", appID);
            statisticsObject.put("serialNumber", serialNumber);
            statisticsObject.put("apiVersion", apiVersion);
            statisticsObject.put("buildRelease", buildRelease);
            statisticsObject.put("osVersion", osVersion);
            statisticsObject.put("device", device);
            statisticsObject.put("model", model);
            statisticsObject.put("product", product);
            statisticsObject.put("operatorName", operatorName);
            statisticsObject.put("SIMOperatorName", SIMOperatorName);
            statisticsObject.put("Location long", locationLong);
            statisticsObject.put("Location lat", locationLat);


            String compressedPackage = null;
            try {
                //compressedPackage = statisticsObject.toString().replace("\"","\\\""); // w/o compression
                compressedPackage = Compression.DeflateCompress(statisticsObject.toString());
                Encryption encryption = new Encryption();
                String encryptedPackage = encryption.Encrypt(compressedPackage);

                String token=prefs.getString("token", "notGeneratedYet");
                if(token.equals("notGeneratedYet")) {
                    prefs.edit().putString("encryptedPackage",compressedPackage).apply();
                    //stop method
                    return;
                }else{
                    //to avoid send data again in OnNewToken method
                    prefs.edit().putBoolean("firstGenratedToken",false).apply();
                }

                JSONObject data = new JSONObject();
                data.put("encryptedPackage", encryptedPackage);
                data.put("token", prefs.getString("token", ""));

                /*TODO Send statisticsObject & token as string*/
                String data_str  = data.toString().replace("\"","\\\"");

                Log.e("UploadStatistics data", data_str);
                new UploadStatisticsTask().execute(data_str);

                Log.e("compression", compressedPackage);
                Log.e("encryption", encryptedPackage);
                Log.e("SecretKey", encryption.getEncryptedSecretKey());
                Log.e("token+info", data_str);

            } /*catch (IOException e) { //compressedPackage);//
                e.printStackTrace();
            }*/ catch (Exception e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }



        public static class UploadStatisticsTask extends AsyncTask<String,String,String>
        {
            @Override
            protected void onPreExecute()
            {
            }

            @Override
            protected String doInBackground(String... params)
            {
                String response = UploadStatistics(params[0]);

                Log.e("UploadStatisticsRespon", response);
                return response;
            }

            @Override
            protected void onPostExecute(String result)
            {
                super.onPostExecute(result);
            }


            public String UploadStatistics(String data)
            {
                java.net.URL url = null;
                StringBuilder builder = new StringBuilder();
                HttpURLConnection connection = null;
                try {
                    // create url
                    //http://172.20.10.10:65079/api/monitoringtool/
                    url = new URL("http://framework1-001-site1.htempurl.com/api/monitoringtool/UploadStatistics");
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
