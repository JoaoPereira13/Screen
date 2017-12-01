package com.example.screen;

import android.content.Context;
import android.location.Location;
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


public class Demo extends AppCompatActivity implements PermissionsListener {

    private static final String TAG = "debugAPP";
    private static final int refreshPeriodMs = 1000;
    private static final int initialDelayMs = 2000;
    private static final int zoomLevel = 17;
    private static final int receivePort = 13892;
    private static final int destinyPort = 13891;
    private static final int opposingLaneThreshold = 45;
    private static final int sameLaneThreshold = 45;
    private static final int inFrontThreshold = 45;
    private static final int cameraAnimationTimeMs = 600;

    private static final boolean enableTracking = true;

    private static final int MARKER_COLOR_PURPLE        = 0;
    private static final int MARKER_COLOR_RED           = 1;
    private static final int MARKER_COLOR_LIGHT_BLUE    = 2;
    private static final int MARKER_COLOR_GREEN         = 3;
    private static final String TSIG_STOP                  = "TS_STOP";
    private static final String TSIG_TRAFFIC_LIGHT_GREEN   = "TS_TL_GREEN";
    private static final String TSIG_TRAFFIC_LIGHT_YELLOW  = "TS_TL_YELLOW";
    private static final String TSIG_TRAFFIC_LIGHT_RED     = "TS_TL_RED";
    private static final String TSIG_WORK_IN_PROGRESS      = "TS_WIP";

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap map;

    private WifiManager wifi;
    private ReceiveData receiveData;

    private VehicularTable vehicularTable = null;
    private TrafficSignalTable trafficSignalTable = null;

    private Marker userMarker = null;
    private Hashtable<String, Marker> neighbourMarkers;
    private Hashtable<String, Marker> trafficSignalMarkers;

    private static Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Demo.this.runOnUiThread(new Runnable() {
                /** Loop function */
                public void run() {

                    DEBUG_addUserData();

                    /** Update the user and neighbours markers positions */
                    if(vehicularTable.isUserDataAvailable()) {
                        DEBUG_addData();
                        refreshMarkers(vehicularTable.getUserData(), vehicularTable.getNeighboursData());

                        trafficSignalTable.updateTime();
                        refreshTrafficSignalMarkers(trafficSignalTable.getTrafficSignalsData());
                    }
                }
            });
            handler.postDelayed(this, refreshPeriodMs);
        }
    };

    private void DEBUG_addUserData(){
        vehicularTable.updateVehicularTable(new VehicularData("000", "-8.692938", "40.630822", "81.600000", "253.2"));
    }

    private void DEBUG_addData(){
        /** User Data */
        vehicularTable.updateVehicularTable(new VehicularData("000", "-8.692938", "40.630822", "81.600000", "253.2"));

        /** Neighboring Vehicles Data */
        vehicularTable.updateVehicularTable(new VehicularData("001", "-8.692694", "40.630883", "80.833333", "253.3"));
        vehicularTable.updateVehicularTable(new VehicularData("002", "-8.693373", "40.630693", "80.111111", "255.5"));

        vehicularTable.updateVehicularTable(new VehicularData("003", "-8.692094", "40.630883", "80.833333", "73.3"));

        vehicularTable.updateVehicularTable(new VehicularData("004", "-8.692973", "40.630693", "80.111111", "75.5"));

        /** Traffic Signals Data */
        // trafficSignalTable.updateTrafficSignalTable(new TrafficSignalData("001", "-8.693373", "40.630693", "255.5", "TS_STOP"));
    }

    private void init(){
        Log.i(TAG,"\nPermissions granted. Starting the app...");

        trafficSignalTable = new TrafficSignalTable();
        vehicularTable = new VehicularTable();
        vehicularTable.setUserId("000");

        /** Start the thread responsible for receiving the GPS data from the OBU */
        //receiveData = new ReceiveData(receivePort, destinyPort, getUsrIP(), vehicularTable, trafficSignalTable);  /** TO DEBUG COMMENT THIS */
        //receiveData = new ReceiveData(receivePort, destinyPort, "10.6.85.1", vehicularTable, trafficSignalTable);  /** TO DEBUG UNCOMMENT THIS */

        //receiveData.execute();

        /** Initialize the lists containing the neighbour and traffic signal markers */
        neighbourMarkers = new Hashtable<String, Marker>();
        trafficSignalMarkers = new Hashtable<String, Marker>();

    }

    private void refreshMarkers(VehicularData userData, List<VehicularData> neighbourDataList){
        refreshUserMarker(userData);
        refreshNeighbourMarkers(userData, neighbourDataList);           /** User Data is used to determine the neighbor lane */
    }

    private void refreshUserMarker(VehicularData userData){
        if(enableTracking) {
            if(userMarker == null){                                     /** First user position */
                /** Get the user icon */
                IconFactory iconFactory = IconFactory.getInstance(Demo.this);
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
            addNeighbourMarker(userData, neighbourData);
        }
        else if(vehicularTable.isUserDataAvailable()){
            moveNeighMarker(neighbourData, getNeighMarkerColor(userData, neighbourData));
        }
    }

    private int getNeighMarkerColor(VehicularData userData, VehicularData neighbourData){

        if(isInFront(userData.getLatLng(),neighbourData.getLatLng(), userData.getCourseDouble())) {
            if(isOnOpposingLane(userData.getCourseDouble(), neighbourData.getCourseDouble())) {
                return MARKER_COLOR_RED;
            }
            else if(isOnSameLane(userData.getCourseDouble(), neighbourData.getCourseDouble())) {
                    return MARKER_COLOR_LIGHT_BLUE;
            }
        }
            return MARKER_COLOR_GREEN;
    }
    private boolean isInFront(LatLng X, LatLng Y, double course){
        double longitude1 = X.getLongitude();
        double longitude2 = Y.getLongitude();
        double latitude1 = Math.toRadians(X.getLatitude());
        double latitude2 = Math.toRadians(X.getLatitude());
        double longDiff= Math.toRadians(longitude2-longitude1);
        double y = Math.sin(longDiff)*Math.cos(latitude2);
        double x =Math.cos(latitude1)*Math.sin(latitude2)-Math.sin(latitude1)*Math.cos(latitude2)*Math.cos(longDiff);

        double bearing = (Math.toDegrees(Math.atan2(y, x))+360)%360;

        //Log.i(TAG,"\nBearing = "+bearing+" User Course: "+course+" |bearing - userCourse| = "+Math.abs(bearing - course));
        double dif = Math.abs(bearing - course);

        return dif < inFrontThreshold || dif > (360 - inFrontThreshold);
    }

    private boolean isOnOpposingLane(Double userCourse, Double neighCourse){
        Double usrOppositeCourse = (userCourse + 180) % 360;
        Double dif = Math.abs(neighCourse - usrOppositeCourse);
        return dif < opposingLaneThreshold || dif > (360 - opposingLaneThreshold);
    }
    private boolean isOnSameLane(Double userCourse, Double neighCourse){
        double dif = Math.abs(neighCourse - userCourse);
        return dif < sameLaneThreshold || dif > (360 - sameLaneThreshold);
    }

    private void addNeighbourMarker(VehicularData userData, VehicularData neighbourData){
        /** Get the neighbor icon */
        int markerColor = getNeighMarkerColor(userData, neighbourData);
        IconFactory iconFactory = IconFactory.getInstance(Demo.this);
        Icon icon = null;

        if(markerColor == MARKER_COLOR_RED)
            icon = iconFactory.fromResource(R.mipmap.red_round_marker);

        if(markerColor == MARKER_COLOR_LIGHT_BLUE)
            icon = iconFactory.fromResource(R.mipmap.light_blue_round_marker);

        if(markerColor == MARKER_COLOR_GREEN)
            icon = iconFactory.fromResource(R.mipmap.green_round_marker);

        neighbourMarkers.put(neighbourData.getId(), map.addMarker(new MarkerViewOptions()
                .position(neighbourData.getLatLng())
                .icon(icon)));
    }

    private void moveNeighMarker(VehicularData neighbourData, int markerColor){
        String id = neighbourData.getId();
        IconFactory iconFactory = IconFactory.getInstance(Demo.this);
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

    private void refreshTrafficSignalMarkers(List<TrafficSignalData> trafficSignalDataList){
        int i = 0;
        while( i < trafficSignalDataList.size()){
            refreshTrafficSignalMarker(trafficSignalDataList.get(i));
            i++;
        }
    }

    private void refreshTrafficSignalMarker(TrafficSignalData trafficSignalData){

        if(!trafficSignalMarkers.containsKey(trafficSignalData.getId())) {               /** New marker */

            if (trafficSignalData.getTTL() != 0) {
                addTrafficSignalMarker(trafficSignalData);
            }
        }
        else {
            if(trafficSignalData.getTTL() == 0){
                removeTrafficSignalMarker(trafficSignalData.getId());
            }
            else {
                String type = trafficSignalData.getType();
                if (type.equals(TSIG_STOP)) {
                    updateTrafficSignalMarker(trafficSignalData, TSIG_STOP);
                } else if (type.equals(TSIG_TRAFFIC_LIGHT_GREEN))
                    updateTrafficSignalMarker(trafficSignalData, TSIG_TRAFFIC_LIGHT_GREEN);

                else if (type.equals(TSIG_TRAFFIC_LIGHT_YELLOW))
                    updateTrafficSignalMarker(trafficSignalData, TSIG_TRAFFIC_LIGHT_YELLOW);

                else if (type.equals(TSIG_TRAFFIC_LIGHT_RED))
                    updateTrafficSignalMarker(trafficSignalData, TSIG_TRAFFIC_LIGHT_RED);

                else if (type.equals(TSIG_WORK_IN_PROGRESS))
                    updateTrafficSignalMarker(trafficSignalData, TSIG_WORK_IN_PROGRESS);
            }
        }
    }

    private void removeTrafficSignalMarker(String id){
        /** update the marker */
        map.removeMarker(trafficSignalMarkers.get(id));
        trafficSignalMarkers.remove(id);
    }

    private void addTrafficSignalMarker(TrafficSignalData trafficSignalData){

        String id = trafficSignalData.getId();
        String type = trafficSignalData.getType();

        IconFactory iconFactory = IconFactory.getInstance(Demo.this);
        Icon icon = getTrafficSignalIcon(type);

        trafficSignalMarkers.put(trafficSignalData.getId(), map.addMarker(new MarkerViewOptions()
                .position(trafficSignalData.getLatLng())
                .icon(icon)));
        Log.i(TAG,"\nAdded new traffic signal with type: "+type);
    }

    private void updateTrafficSignalMarker(TrafficSignalData trafficSignalData, String type){
        //
        String id = trafficSignalData.getId();
        IconFactory iconFactory = IconFactory.getInstance(Demo.this);
        Icon icon = getTrafficSignalIcon(type);

        /** update the marker */
        trafficSignalMarkers.get(id).setIcon(icon);
        trafficSignalMarkers.get(id).setPosition(trafficSignalData.getLatLng());
    }

    private Icon getTrafficSignalIcon(String type){
        //TODO GET THE ICONS FOR TRAFFIC SIGNALS
        IconFactory iconFactory = IconFactory.getInstance(Demo.this);

        if(type.equals(TSIG_STOP))
            return iconFactory.fromResource(R.mipmap.red_round_marker);

        else if(type.equals(TSIG_TRAFFIC_LIGHT_GREEN))
            return iconFactory.fromResource(R.mipmap.red_round_marker);

        else if(type.equals(TSIG_TRAFFIC_LIGHT_YELLOW))
            return iconFactory.fromResource(R.mipmap.red_round_marker);

        else if(type.equals(TSIG_TRAFFIC_LIGHT_RED))
            return iconFactory.fromResource(R.mipmap.red_round_marker);

        else if(type.equals(TSIG_WORK_IN_PROGRESS))
            return iconFactory.fromResource(R.mipmap.red_round_marker);
        else
            return null;
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

                permissionsManager = new PermissionsManager(Demo.this);
                if (!PermissionsManager.areLocationPermissionsGranted(Demo.this)) {
                    permissionsManager.requestLocationPermissions(Demo.this);
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
        // receiveData.cancel(true); // DEBUG - COMMENT
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