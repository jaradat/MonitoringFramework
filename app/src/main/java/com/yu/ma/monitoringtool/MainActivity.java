package com.yu.ma.monitoringtool;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.ma.monitoringlibrary.Measurement;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;


public class MainActivity extends AppCompatActivity {

    private Measurement measurement;
    int idMeasurement = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Measurement.init(this);

        measurement = new Measurement(this);


        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(1024); // key length
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            String privateKeyString = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);
            String publicKeyString = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);

            // 2. print both keys
            Log.e("keyS", "privateKey\n" + privateKeyString + "\n");
            Log.e("keyS", "publicKey\n" + publicKeyString + "\n\n");


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }


    }

    public Bundle getMetaData(Context context) {
        try {
            return context.getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting:
                Measurement.showSettingActivity(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void start_end(View view) {
        int id = view.getId();
        if (id == R.id.start)
            idMeasurement = measurement.start(Measurement.Type.HTTP_REQUEST);
        else
            measurement.end(Measurement.Type.HTTP_REQUEST, idMeasurement);

    }
}
