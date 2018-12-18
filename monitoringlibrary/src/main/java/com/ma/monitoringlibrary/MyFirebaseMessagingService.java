package com.ma.monitoringlibrary;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private SharedPreferences prefs;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with GCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (/* Check if data needs to be processed by long running job */ true) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
            } else {
                // Handle message within 10 seconds
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            Map<String, String> params = remoteMessage.getData();
            JSONObject object = new JSONObject(params);
            String type_name = null;
            float percentage = 1;
            try {
                type_name = object.getString("type_name");
                percentage = Float.valueOf(object.getString("percentage"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //rest of the code
            prefs.edit().putFloat(type_name + " percentage",percentage).apply();
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    // [START on_new_token]

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        SharedPreferences prefs = getApplicationContext().getApplicationContext().getSharedPreferences(getApplicationContext().getString(R.string.monitoringPref), MODE_PRIVATE);
        prefs.edit().putString("token", token).apply();

        ApplicationInfo app = null;
        try {
            app = getApplicationContext().getPackageManager().getApplicationInfo(getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Bundle bundle = app.metaData;
        int developerID = bundle.getInt("DeveloperID");
        String appID = bundle.getString("AppID");

        prefs.edit().putInt("developerID",developerID).apply();
        prefs.edit().putString("appID",appID).apply();

        boolean firstGenrateToken = prefs.getBoolean("firstGenrateToken", true);
        if (firstGenrateToken) {
            prefs.edit().putBoolean("firstGenrateToken", false).apply();
            String compressedPackage = prefs.getString("encryptedPackage", "notCollectedYet");
            if (!compressedPackage.equals("notCollectedYet")) {

                JSONObject data = new JSONObject();
                try {
                    data.put("encryptedPackage", compressedPackage);//encryptedPackage);
                    data.put("token", prefs.getString("token", ""));

                    Log.e("compression from token", compressedPackage);
                    String data_str = data.toString().replace("\"", "\\\"");
                    new Configuration.UploadStatisticsTask().execute(data_str);//encryptedPackage);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

        } else {
            //TODO update token on server
            String serialNumber = prefs.getString("serialNumber","");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            String currentDateTime = sdf.format(new Date());
            JSONObject tokenObject = new JSONObject();
            try {
                tokenObject.put("developerID", developerID);
                tokenObject.put("appID", appID);
                tokenObject.put("serialNumber", serialNumber);
                tokenObject.put("token", token);
                tokenObject.put("timestamp", currentDateTime);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // do what you want
            new UpdateTokenTask().execute(tokenObject.toString());
        }
    }


    public class UpdateTokenTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... params) {
            String response = updateToken(params[0]);

            Log.e("uploadToken", response);
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        public String updateToken(String data) {
            URL url = null;
            StringBuilder builder = new StringBuilder();
            HttpsURLConnection connection = null;
            try {
                // create url
                url = new URL("http://framework1-001-site1.htempurl.com/api/monitoringtool/UpdateToken");
                // open a connection
                connection = (HttpsURLConnection) url.openConnection();

                connection.setDoInput(true); // to get request only
                connection.setDoOutput(true); // upload a request body
                connection.setUseCaches(false);
                connection.setRequestMethod("POST"); // request method post
                connection.setRequestProperty("Content-Type", "application/json");
                //connection.setRequestProperty("Content-Length","" + Integer.toString(type.getBytes().length));
                connection.setRequestProperty("Content-Language", "en-US");
                connection.setConnectTimeout(30000); // connection time out
                connection.setReadTimeout(30000); // read time out


                // Send request
                String json = "{\"data\":\"" + data + "\"}";
                connection.setRequestProperty("Content-Length", "" + Integer.toString(json.getBytes().length));
                OutputStream outStream = new BufferedOutputStream(connection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, "UTF-8"));
                writer.write(json);
                writer.flush();
                writer.close();
                outStream.close();

                // get response
                InputStream inStream = connection.getInputStream(); // input stream of connection to get data
                BufferedReader reader = new BufferedReader(new InputStreamReader(inStream)); // reader for reading data from stream
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                int responseCode = connection.getResponseCode();
                reader.close();
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    return builder.toString();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return builder.toString();
        }

    }

}
