package com.ma.monitoringlibrary.SettingActivity;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.ma.monitoringlibrary.R;

public class SettingActivity extends AppCompatActivity {


    private Switch location_sw, libs_sw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final SharedPreferences preferences = getSharedPreferences(getString(R.string.monitoringPref), MODE_PRIVATE);

        location_sw = findViewById(R.id.GpsSwitch);
        libs_sw = findViewById(R.id.libsSwitch);

        location_sw.setChecked(preferences.getBoolean(getString(R.string.GpsStatus), false));
        libs_sw.setChecked(preferences.getBoolean(getString(R.string.ToolsStatus), false));

        location_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(getString(R.string.GpsStatus), isChecked).apply();
            }
        });

        libs_sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preferences.edit().putBoolean(getString(R.string.ToolsStatus), isChecked).apply();
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onBackPressed() {
        finish();
    }


}
