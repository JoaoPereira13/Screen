package com.example.screen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "debug";
    private final static int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    private Button btnDemo;
    private Button btnMap;
    private Button btnTrack;
    private Button btnCM;
    private Button btnOffMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askForPermissions();

        btnDemo = (Button) findViewById(R.id.btnDemo);
        btnMap = (Button) findViewById(R.id.btnMap);
        btnTrack = (Button) findViewById(R.id.btnTrackMap);

        btnCM = (Button) findViewById(R.id.btnCM);
        btnOffMap = (Button) findViewById(R.id.btnOffline);

        /** Button Demo Listener*/
        btnDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnDemo) {
                    Log.i(TAG,"Demo Button Listener");

                    Intent intent = new Intent(MainActivity.this,
                            Demo.class);
                    startActivity(intent);
                }
            }
        });

        /** Button View Map Listener*/
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnMap) {
                    Log.i(TAG,"Map button Listener");

                    Intent intent = new Intent(MainActivity.this,
                            ViewMap.class);
                    startActivity(intent);
                }
            }
        });

        /** Button Tracking mode*/
        btnTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnTrackMap) {
                    Log.i(TAG,"Tracking Mode Button Listener");

                    Intent intent = new Intent(MainActivity.this,
                            TrackCustomRoute.class);
                    startActivity(intent);
                }
            }
        });
        /** Button Offline Map Manager */
        btnOffMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnOffline) {
                    Log.i(TAG,"Offline Map Manager Button Listener");

                    Intent intent = new Intent(MainActivity.this, OfflineMap.class);
                    startActivity(intent);
                }
            }
        });

        /** Button Connection Management Listener*/
        btnCM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnCM) {
                    Log.i(TAG,"Connection Manager Button Listener");

                    Intent intent = new Intent(MainActivity.this, ConnectionManagement.class);
                    startActivity(intent);
                }
            }
        });
    }

    private void askForPermissions(){
        Log.i(TAG,"Entering askForPermissions()...");

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"Permission Granted...");
                } else {
                    Log.i(TAG,"Permission Not Granted...");
                }
                return;
            }
        }
    }
}
