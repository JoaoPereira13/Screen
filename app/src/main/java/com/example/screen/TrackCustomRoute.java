package com.example.screen;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
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

import java.util.ArrayList;
import java.util.List;


public class TrackCustomRoute extends AppCompatActivity implements PermissionsListener {

    private static final String TAG = "debug";
    private static final int refreshPeriodMs = 500;
    private static final int initialDelayMs = 2000;
    private static final int zoomLevel = 16;
    private static final int receivePort = 13892;
    private static final int destinyPort = 13891;
    private static final int opposingLaneThreshold = 5;
    private static final int sameLaneThreshold = 5;
    private static final int markerAnimationTimeMs = 600;
    private static final int cameraAnimationTimeMs = 600;

    private static final boolean enableTracking = true;

    private static final int MARKER_COLOR_PURPLE = 0;
    private static final int MARKER_COLOR_RED = 1;
    private static final int MARKER_COLOR_LIGHT_BLUE = 2;
    private static final int MARKER_COLOR_GREEN = 3;

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap map;
    private GpsData userData = null;
    private GpsData neighData;
    private WifiManager wifi;

    private Marker userMarker = null;
    private List<Marker> neighMarkers;
    private List<String> neighsIndex = null;
    private NodesData nodesData = null;
    private ReceiveGpsData receiveGpsData;

    private static Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            TrackCustomRoute.this.runOnUiThread(new Runnable() {
                /** Loop function */
                public void run() {

                    /** Update the nodes positions */
                    int i = 0;
                    while(i < nodesData.nodesIndex.size()) {
                        if(!nodesData.nodesIndex.get(i).equals(nodesData.getUserId())){         /** Neighbor position */
                            neighData = nodesData.getNeighData(nodesData.nodesIndex.get(i));
                            refreshNeighPosition(nodesData.nodesIndex.get(i));
                        }
                        else if(nodesData.isUserDataAvailable()){                               /**  User position */
                            userData = nodesData.getUserData();
                            refreshUserPosition();
                        }
                        i++;
                    }
                }
            });
            handler.postDelayed(this, refreshPeriodMs);
        }
    };

    private void init(){
        Log.i(TAG,"\nPermissions granted. Starting the app...");

        neighData = new GpsData();
        userData = new GpsData();
        nodesData = new NodesData();
        nodesData.setUserId(getUsrId());

        /** Start the thread responsible for receiving the GPS data from the OBU */
        receiveGpsData= new ReceiveGpsData(receivePort, destinyPort, getUsrIP(), nodesData);
        receiveGpsData.execute();

        Log.i(TAG,"isCanceled: "+receiveGpsData.isCancelled());


        /** Initialize the lists containing the neighbor markers and index */
        neighMarkers = new ArrayList<>();
        neighsIndex = new ArrayList<>();
    }

    private void refreshUserPosition(){
        if(enableTracking) {
            if(userMarker==null){
                /** Get the user icon */
                IconFactory iconFactory = IconFactory.getInstance(TrackCustomRoute.this);
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
                    .bearing(Double.parseDouble(nodesData.getUserData().getCourse()))
                    .build()),cameraAnimationTimeMs);
            /** Move the user marker */
            ValueAnimator markerAnimator = ObjectAnimator.ofObject(userMarker, "position",
                    new LatLngEvaluator(), userMarker
                            .getPosition(), userData.getLatLng());

            markerAnimator.setDuration(markerAnimationTimeMs);
            markerAnimator.start();
        }
    }

    private void refreshNeighPosition(String id) {
        /** Create the Neighbor marker */
        if (getIndex(id) == -1) {           /** New marker */
            addNeighMarker(id);
        }
        else if(nodesData.isUserDataAvailable()){
            /** Car in the same lane */
            if(isOnOpposingLane(userData.getCourseDouble(), neighData.getCourseDouble()))
                moveNeighMarker(id, MARKER_COLOR_RED);

            /** Car in the opposing lane */
            else if(isOnSameLane(userData.getCourseDouble(), neighData.getCourseDouble()))
                moveNeighMarker(id, MARKER_COLOR_LIGHT_BLUE);

            else
                moveNeighMarker(id, MARKER_COLOR_GREEN);
        }
    }

    private void moveNeighMarker(String id, int markerColor){

        IconFactory iconFactory = IconFactory.getInstance(TrackCustomRoute.this);
        Icon icon = null;

        if(markerColor == MARKER_COLOR_RED)
            icon = iconFactory.fromResource(R.mipmap.red_round_marker);

        if(markerColor == MARKER_COLOR_LIGHT_BLUE)
            icon = iconFactory.fromResource(R.mipmap.light_blue_round_marker);

        if(markerColor == MARKER_COLOR_GREEN)
            icon = iconFactory.fromResource(R.mipmap.green_round_marker);

        /** Set the marker color */
        neighMarkers.get(getIndex(id)).setIcon(icon);

        /** Move the neighbor marker */
        ValueAnimator markerAnimator = ObjectAnimator.ofObject(neighMarkers.get(getIndex(id)), "position",
                new LatLngEvaluator(), neighMarkers.get(getIndex(id))
                        .getPosition(), neighData.getLatLng());

        markerAnimator.setDuration(markerAnimationTimeMs);
        markerAnimator.start();
    }

    private void addNeighMarker(String id){
        /** Get the neighbor icon */
        IconFactory iconFactory = IconFactory.getInstance(TrackCustomRoute.this);
        Icon icon = iconFactory.fromResource(R.mipmap.green_round_marker);

        neighsIndex.add(id);
        neighMarkers.add(map.addMarker(new MarkerViewOptions()
                .position(neighData.getLatLng())
                .icon(icon)));
    }

    private String getUsrIP(){
        /** Extract the IP address of the OBU from the SSID */
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        Log.i(TAG,"SSID: "+info.getSSID());
        String[] separated;
        separated = info.getSSID().toString().split("netRider");
        return "10."+separated[1].substring(0,1)+"."+separated[1].substring(1,3)+".1";
    }

    private String getUsrId(){
        /** Extract the ID address of the OBU from the SSID */
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        Log.i(TAG,"SSID: "+info.getSSID());
        String[] separated;
        separated = info.getSSID().toString().split("netRider");
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

    private int getIndex(String id) {
        return neighsIndex.indexOf(id);
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

                permissionsManager = new PermissionsManager(TrackCustomRoute.this);
                if (!PermissionsManager.areLocationPermissionsGranted(TrackCustomRoute.this)) {
                    permissionsManager.requestLocationPermissions(TrackCustomRoute.this);
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
        receiveGpsData.cancel(true);
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