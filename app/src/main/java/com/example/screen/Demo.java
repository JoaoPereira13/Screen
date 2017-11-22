package com.example.screen;


import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Demo extends AppCompatActivity implements PermissionsListener {

    private static final String TAG = "debug";
    private static final int refreshPeriodMs = 4000;
    private static final int initialDelayMs = 2000;
    private static final int zoomLevel = 18;
    private static final int opposingLaneThreshold = 60;
    private static final int sameLaneThreshold = 20;
    private static final int sampleDelay = 7;
    private static final String userId = "685";
    private static final String neighDelay = "683";
    private static final String fileName = "T5_4.txt";

    private static int posGen = 0;
    private static int sampleDelayAux = 0;

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap map;

    private MarkerViewOptions userMarker = null;
    private List<VehicularData> usrPoints;
    private List<List<VehicularData>> neighPoints;
    private List<MarkerViewOptions> neighMarkers;
    private List<String> neighsIndex = null;
    private List<Integer> neighPosGen;
    private VehicularData userData;
    private VehicularData neighData;
    private boolean isVisible = true;


    private static Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Demo.this.runOnUiThread(new Runnable() {
                /** Loop function */
                public void run() {

                    /** Get the next sample */
                    userData = getNextPosition();

                    /** Update the user position */
                    refreshUserPosition();

                    /** Update the neighbors position */
                    int i = 0;
                    while(i < neighsIndex.size()) {
                        if(neighsIndex.get(i).equals(neighDelay)){
                            if(sampleDelayAux >= sampleDelay) {
                                neighData = getNextNeighPosition(neighsIndex.get(i));
                                refreshNeighPosition(neighsIndex.get(i));
                            }
                        }
                        else{
                                neighData = getNextNeighPosition(neighsIndex.get(i));
                            refreshNeighPosition(neighsIndex.get(i));
                        }
                        i++;
                        sampleDelayAux++;
                    }
                }
            });
            handler.postDelayed(this, refreshPeriodMs);
        }
    };

    private void init() {
        Log.i(TAG, "\nPermissions granted. Starting the app...");

        /** Initialize the Lists */
        usrPoints = new ArrayList<>();
        neighPoints = new ArrayList<List<VehicularData>>();
        neighMarkers = new ArrayList<>();
        neighsIndex = new ArrayList<>();
        neighPosGen = new ArrayList<>();


        /** Get the Sample coordinates */
        getSampleFromData();
    }

    private void refreshUserPosition(){

        if(userMarker == null){
            /** Get the user icon */
            IconFactory iconFactory = IconFactory.getInstance(Demo.this);
            Icon icon = iconFactory.fromResource(R.mipmap.purple_round_marker);

            /** Add the user marker in the starting position */
            userMarker = new MarkerViewOptions()
                    .position(userData.getLatLng())
                    .icon(icon);

            /** Move the camera to the starting position */
            map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(userData.getLatLng())
                    .zoom(zoomLevel)
                    .bearing(userData.getCourseDouble())
                    .build()));
            map.addMarker(userMarker);
            return;
        }

        /** Move the camera along the user position */
        map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(userData.getLatLng())
                .bearing(userData.getCourseDouble())
                .build()));
        /** Move the user marker */
        map.updateMarker(userMarker
                .position(userData.getLatLng()).getMarker());
    }

    private void refreshNeighPosition(String id) {
        /** Car in the same lane */

        if(isOnOpposingLane(userData.getCourseDouble(), neighData.getCourseDouble())){
            IconFactory iconFactory = IconFactory.getInstance(Demo.this);
            Icon icon = iconFactory.fromResource(R.mipmap.red_round_marker);

            //TODO ONLY FOR THE VIDEO - REMOVE AFTER
            if(neighData.getId().equals("657")){
                iconFactory = IconFactory.getInstance(Demo.this);
                icon = iconFactory.fromResource(R.mipmap.green_round_marker);
            }
            else if(neighData.getId().equals("681")){
                iconFactory = IconFactory.getInstance(Demo.this);
                icon = iconFactory.fromResource(R.mipmap.red_round_marker);
            }
            //

            map.updateMarker(neighMarkers.get(getIndex(id))
                    .position(neighData.getLatLng())
                    .icon(icon)
                    .visible(isVisible)
                    .getMarker());
        }
        /** Car in the opposing lane */
        else if(isOnSameLane(userData.getCourseDouble(), neighData.getCourseDouble())){
            IconFactory iconFactory = IconFactory.getInstance(Demo.this);
            Icon icon = iconFactory.fromResource(R.mipmap.light_blue_round_marker);

            //TODO ONLY FOR THE VIDEO - REMOVE AFTER
            if(neighData.getId().equals("657")){
                iconFactory = IconFactory.getInstance(Demo.this);
                icon = iconFactory.fromResource(R.mipmap.green_round_marker);
            }
            else if(neighData.getId().equals("681")){
                iconFactory = IconFactory.getInstance(Demo.this);
                icon = iconFactory.fromResource(R.mipmap.red_round_marker);
            }
            //

            map.updateMarker(neighMarkers.get(getIndex(id))
                    .position(neighData.getLatLng())
                    .icon(icon)
                    .getMarker());
        }
        else{
            IconFactory iconFactory = IconFactory.getInstance(Demo.this);
            Icon icon = iconFactory.fromResource(R.mipmap.green_round_marker);
            //TODO ONLY FOR THE VIDEO - REMOVE AFTER
            if(neighData.getId().equals("657")){
                iconFactory = IconFactory.getInstance(Demo.this);
                icon = iconFactory.fromResource(R.mipmap.green_round_marker);
            }
            else if(neighData.getId().equals("681")){
                iconFactory = IconFactory.getInstance(Demo.this);
                icon = iconFactory.fromResource(R.mipmap.red_round_marker);
            }
            //


            map.updateMarker(neighMarkers.get(getIndex(id))
                    .position(neighData.getLatLng())
                    .icon(icon)
                    .getMarker());
        }
    }

    private void addNeighMarker(String id){
        /** Get the neighbor icon */
        IconFactory iconFactory = IconFactory.getInstance(Demo.this);
        Icon icon = iconFactory.fromResource(R.mipmap.green_round_marker);

        /** Create the marker */
        neighMarkers.add(new MarkerViewOptions().position(new LatLng(-1,-1)).icon(icon));
        neighsIndex.add(id);

        /** Add the new marker to the map */
        map.addMarker(neighMarkers.get(getIndex(id)));

        /** Initialize the List containing all the positions for the new neighbor*/
        neighPoints.add(new ArrayList<VehicularData>());
        neighPosGen.add(0);
    }

    private void addNeighPos(VehicularData vehicularData){
        neighPoints.get(getIndex(vehicularData.getId())).add(vehicularData);
    }

    private VehicularData getNextPosition(){
        if(posGen == usrPoints.size()-1){
            /** When the animation ends -> Restart */
            posGen = 0;
            int i = 0;
            sampleDelayAux = 0;
            isVisible = true;
            while(i < neighsIndex.size()){
                neighPosGen.set(i, 0);
                i++;
            }
        }
        return usrPoints.get(++posGen);
    }

    private VehicularData getNextNeighPosition(String id){

        if(neighPosGen.get(getIndex(id)) == neighPoints.get(getIndex(id)).size()-1) {
            Log.i(TAG,"entered if, id: "+id);
            //return neighPoints.get(getIndex(id)).get(neighPosGen.get(getIndex(id)));
            return new VehicularData(neighDelay,"-1","-1","-1","-1");
        }

        VehicularData vehicularData = neighPoints.get(getIndex(id)).get(neighPosGen.get(getIndex(id)));
        neighPosGen.set(getIndex(id), neighPosGen.get(getIndex(id)) +1);
        return vehicularData;
    }

    private int getIndex(String id) {
        return neighsIndex.indexOf(id);
    }

    private void getSampleFromData(){
        BufferedReader reader = null;
        String[] separated;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open(fileName)));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                /** Seperate the Latitude from Longitude data */
                separated = mLine.split(" ");
                String id = separated[1];
                if(id.equals(userId)) {             /** User Data */
                    usrPoints.add(new VehicularData(
                            separated[1],
                            separated[3],
                            separated[2],
                            separated[4],
                            separated[5]));
                }
                else{                               /** Neighbors Data */
                    if( getIndex(id) == -1) {
                        addNeighMarker(id);
                    }
                    addNeighPos(new VehicularData(
                            separated[1],
                            separated[3],
                            separated[2],
                            separated[4],
                            separated[5]));
                }
            }
        } catch (IOException e) {
            /** log the exception */
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    /** log the exception */
                }
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /** Mapbox access token configuration */
        Mapbox.getInstance(this, getString(R.string.access_token));

        /** This contains the MapView in XML and needs to be called after the account manager */
        setContentView(R.layout.activity_demo_version);

        /** Get the mapView object reference */
        mapView = (MapView) findViewById(R.id.mapDemo);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {

                map = mapboxMap;
                map.getUiSettings().setAllGesturesEnabled(false);

                permissionsManager = new PermissionsManager(Demo.this);
                if (!PermissionsManager.areLocationPermissionsGranted(Demo.this)) {
                    permissionsManager.requestLocationPermissions(Demo.this);
                } else{
                    init();
                }
            }
        });
    }

    private boolean isOnOpposingLane(Double userCourse, Double neighCourse){
        Double usrOppositeCourse = (userCourse + 180) % 360;
        return Math.abs(neighCourse - usrOppositeCourse) < opposingLaneThreshold ;
    }
    private boolean isOnSameLane(Double userCourse, Double neighCourse){
        Double usrOppositeCourse = (userCourse + 180) % 360;
        Double neighOppositeCourse = (neighCourse + 180) % 360;

        return Math.abs(neighOppositeCourse - usrOppositeCourse) < sameLaneThreshold ;
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