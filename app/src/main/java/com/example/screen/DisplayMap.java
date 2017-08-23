package com.example.screen;

import android.animation.TypeEvaluator;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import java.util.Hashtable;
import java.util.List;


public class DisplayMap extends AppCompatActivity implements PermissionsListener {

    private static final String TAG = "debug";
    private static final int refreshPeriodMs = 1000;
    private static final int initialDelayMs = 2000;
    private static final int zoomLevel = 17;
    private static final int receivePort = 13892;
    private static final int destinyPort = 13891;
    private static final int opposingLaneThreshold = 5;
    private static final int sameLaneThreshold = 5;
    private static final int cameraAnimationTimeMs = 600;

    private static final boolean enableTracking = true;

    private static final int MARKER_COLOR_PURPLE = 0;
    private static final int MARKER_COLOR_RED = 1;
    private static final int MARKER_COLOR_LIGHT_BLUE = 2;
    private static final int MARKER_COLOR_GREEN = 3;

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap map;

    private WifiManager wifi;
    private ReceiveData receiveData;

    private VehicularTable vehicularTable = null;
    private TrafficSignalTable trafficSignalTable = null;

    private Marker userMarker = null;
    private Hashtable<String, Marker> neighbourMarkers;

    private static Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            DisplayMap.this.runOnUiThread(new Runnable() {
                /** Loop function */
                public void run() {

                    /** Update the user and neighbours markers positions */
                    if(vehicularTable.isUserDataAvailable())
                        refreshMarkers(vehicularTable.getUserData(), vehicularTable.getNeighboursData());
                }
            });
            handler.postDelayed(this, refreshPeriodMs);
        }
    };

    private void init(){
        Log.i(TAG,"\nPermissions granted. Starting the app...");

        trafficSignalTable = new TrafficSignalTable();
        vehicularTable = new VehicularTable();
        vehicularTable.setUserId("0003");

        /** Start the thread responsible for receiving the GPS data from the OBU */
        receiveData = new ReceiveData(receivePort, destinyPort, getUsrIP(), vehicularTable, trafficSignalTable);
        receiveData.execute();

        /** Initialize the lists containing the neighbour markers */
        neighbourMarkers = new Hashtable<String, Marker>();

    }

    private void refreshMarkers(VehicularData userData, List<VehicularData> neighbourDataList){
        refreshUserMarker(userData);
        refreshNeighbourMarkers(userData, neighbourDataList);           /** User Data is used to determine the neighbor lane */
    }

    private void refreshUserMarker(VehicularData userData){
        if(enableTracking) {
            if(userMarker == null){                                     /** First user position */
                /** Get the user icon */
                IconFactory iconFactory = IconFactory.getInstance(DisplayMap.this);
                Icon icon = iconFactory.fromResource(R.mipmap.purple_round_marker);

                /** Move the camera to the starting position */
                map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target(userData.getLatLng())
                        .zoom(zoomLevel)
                        .bearing(userData.getCourseDouble())
                        .build()));
                /** Add the user marker in the starting position */
                userMarker = map.addMarker( new MarkerViewOptions()
                        .position(userData.getLatLng())
                        .icon(icon));
                return;
            }

            /** Move the camera along the user position */
            map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(userData.getLatLng())
                    .bearing(userData.getCourseDouble())
                    .build()),cameraAnimationTimeMs);

            /** Update the user marker position */
            userMarker.setPosition(userData.getLatLng());

        }
    }

    private void refreshNeighbourMarkers(VehicularData userData, List<VehicularData> neighbourDataList){
        int i = 0;
        while( i < neighbourDataList.size()){
            refreshNeighborMarker(userData, neighbourDataList.get(i));
            i++;
        }
    }

    private void refreshNeighborMarker(VehicularData userData, VehicularData neighbourData) {
        if(!neighbourMarkers.containsKey(neighbourData.getId())){           /** New marker */
            addNeighbourMarker(neighbourData);
        }
        else if(vehicularTable.isUserDataAvailable()){
            /** Car in the same lane */
            if(isOnOpposingLane(userData.getCourseDouble(), neighbourData.getCourseDouble()))
                moveNeighMarker(neighbourData, MARKER_COLOR_RED);

            /** Car in the opposing lane */
            else if(isOnSameLane(userData.getCourseDouble(), neighbourData.getCourseDouble()))
                moveNeighMarker(neighbourData, MARKER_COLOR_LIGHT_BLUE);

            else
                moveNeighMarker(neighbourData, MARKER_COLOR_GREEN);
        }
    }

    private void addNeighbourMarker(VehicularData neighbourData){
        /** Get the neighbor icon */
        IconFactory iconFactory = IconFactory.getInstance(DisplayMap.this);
        Icon icon = iconFactory.fromResource(R.mipmap.green_round_marker);

        neighbourMarkers.put(neighbourData.getId(), map.addMarker(new MarkerViewOptions()
                .position(neighbourData.getLatLng())
                .icon(icon)));
    }

    private void moveNeighMarker(VehicularData neighbourData, int markerColor){
        String id = neighbourData.getId();
        IconFactory iconFactory = IconFactory.getInstance(DisplayMap.this);
        Icon icon = null;

        if(markerColor == MARKER_COLOR_RED)
            icon = iconFactory.fromResource(R.mipmap.red_round_marker);

        if(markerColor == MARKER_COLOR_LIGHT_BLUE)
            icon = iconFactory.fromResource(R.mipmap.light_blue_round_marker);

        if(markerColor == MARKER_COLOR_GREEN)
            icon = iconFactory.fromResource(R.mipmap.green_round_marker);

        /** update the marker */
        neighbourMarkers.get(id).setIcon(icon);
        neighbourMarkers.get(id).setPosition(neighbourData.getLatLng());
    }

    private String getUsrIP(){
        /** Extract the IP address of the OBU from the SSID */
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        String[] separated;
        separated = info.getSSID().toString().split("netRider");
        Log.i(TAG,"IP: "+"10."+separated[1].substring(0,1)+"."+separated[1].substring(1,3)+".1");
        return "10."+separated[1].substring(0,1)+"."+separated[1].substring(1,3)+".1";
    }

    private String getUsrId(){
        /** Extract the ID address of the OBU from the SSID */
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        String[] separated;
        separated = info.getSSID().toString().split("netRider");
        Log.i(TAG,"ID: "+separated[1].substring(0,3));

        return separated[1].substring(0,3);
    }

    private boolean isOnOpposingLane(Double userCourse, Double neighCourse){
        Double usrOppositeCourse = (userCourse + 180) % 360;
        return Math.abs(neighCourse - usrOppositeCourse) < opposingLaneThreshold ;
    }
    private boolean isOnSameLane(Double userCourse, Double neighCourse){
        return Math.abs(neighCourse - userCourse) < sameLaneThreshold ;
    }

    private static class LatLngEvaluator implements TypeEvaluator<LatLng> {
        // Method is used to interpolate the marker animation.

        private LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /** Mapbox access token configuration */
        Mapbox.getInstance(this, getString(R.string.access_token));

        /** This contains the MapView in XML and needs to be called after the account manager */
        setContentView(R.layout.activity_track_custom_route);

        /** Get the mapView object reference */
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

                map = mapboxMap;
                map.getUiSettings().setAllGesturesEnabled(!enableTracking);

                permissionsManager = new PermissionsManager(DisplayMap.this);
                if (!PermissionsManager.areLocationPermissionsGranted(DisplayMap.this)) {
                    permissionsManager.requestLocationPermissions(DisplayMap.this);
                } else{
                    init();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mapView.onStart();
        /** Start the timer */
        handler.postDelayed(runnable, initialDelayMs);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        /** Removes the previously set timer */
        handler.removeCallbacks(runnable);
        /** Cancel the thread receiving the gps data */
        receiveData.cancel(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "This app needs location permissions in order to show its functionality.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {

        } else {
            Toast.makeText(this, "You didn't grant location permissions.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }
}