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
    private static final int refreshPeriodMs = 500;
    private static final int initialDelayMs = 2000;
    private static final int zoomLevel = 17;
    private static final String userId = "0000";

    private static int posGen = 0;
    private static int courseGen = 0;

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap map;
    private LatLng userPosition = null;
    private Double userCourse = null;
    private MarkerViewOptions userMarker = null;
    private List<LatLng> posPoints;
    private List<Double> coursePoints;
    private List<List<LatLng>> neighPos;

    private LatLng neighPosition;
    private List<MarkerViewOptions> neighMarkers;
    private List<String> neighsIndex = null;
    private List<Integer> neighPosGen;

    private static Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Demo.this.runOnUiThread(new Runnable() {
                /** Loop function */
                public void run() {

                    /** Get the next sample */
                    userPosition = getNextPosition();
                    userCourse = getNextCourse();

                    /** Update the user position */
                    refreshUserPosition();

                    /** Update the neighbors position */
                    int i = 0;
                    while(i < neighsIndex.size()) {
                        neighPosition = getNextNeighPosition(neighsIndex.get(i));
                        refreshNeighPosition(neighsIndex.get(i));
                        i++;
                    }
                }
            });
            handler.postDelayed(this, refreshPeriodMs);
        }
    };

    private void init(){
        Log.i(TAG,"\nPermissions granted. Starting the app...");

        /** Initialize the Lists */
        neighPos = new ArrayList<List<LatLng>>();
        neighMarkers = new ArrayList<>();
        neighsIndex = new ArrayList<>();
        neighPosGen = new ArrayList<>();


        /** Get the Sample coordinates */
        getSampleFromData();

        /** Get the user icon */
        IconFactory iconFactory = IconFactory.getInstance(Demo.this);
        Icon icon = iconFactory.fromResource(R.mipmap.purple_round_marker);

        /** Add the user marker in the starting position */
        userPosition = posPoints.get(0);
        userCourse = coursePoints.get(0);

        userMarker = new MarkerViewOptions()
                .position(userPosition)
                .icon(icon);

        /** Move the camera to the starting position */
        map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(userPosition)
                .zoom(zoomLevel)
                .bearing(userCourse)
                .build()));
        map.addMarker(userMarker);
    }

    private void getSampleFromData(){
        BufferedReader reader = null;
        String[] separated;
        posPoints = new ArrayList<>();
        coursePoints = new ArrayList<>();
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("OverT1_Total.txt")));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                /** Seperate the Latitude from Longitude data */
                separated = mLine.split(" ");
                String id = separated[1];
                if(id.equals(userId)) {             /** User Data */
                    posPoints.add(new LatLng(
                            Double.parseDouble(separated[3]),
                            Double.parseDouble(separated[2])));
                    coursePoints.add(Double.parseDouble(separated[5]));
                }
                else{                               /** Neighbors Data */
                    if( getIndex(id) == -1) {
                        addNeighMarker(id);
                    }
                    addNeighPos(id, new LatLng(
                            Double.parseDouble(separated[3]),
                            Double.parseDouble(separated[2])));
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

    private void refreshUserPosition(){

        /** Move the camera along the user position */
        map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(userPosition)
                .bearing(userCourse)
                .build()));
        /** Move the user marker */
        map.updateMarker(userMarker
                .position(userPosition).getMarker());
    }

    private void refreshNeighPosition(String id) {
        /** Move the Neighbor marker */
        map.updateMarker(neighMarkers.get(getIndex(id))
                .position(neighPosition).getMarker());
    }


    private void addNeighMarker(String id){
        /** Get the neighbor icon */
        IconFactory iconFactory = IconFactory.getInstance(Demo.this);
        Icon icon = iconFactory.fromResource(R.mipmap.light_blue_round_marker);

        /** Create the marker */
        neighMarkers.add(new MarkerViewOptions().position(new LatLng(-1,-1)).icon(icon));
        neighsIndex.add(id);

        /** Add the new marker to the map */
        map.addMarker(neighMarkers.get(getIndex(id)));

        /** Initialize the List containing all the positions for the new neighbor*/
        neighPos.add(new ArrayList<LatLng>());
        neighPosGen.add(0);
    }

    private void addNeighPos(String id, LatLng pos){
        neighPos.get(getIndex(id)).add(pos);
    }

    private LatLng getNextPosition(){
        if(posGen == posPoints.size()-1){
            /** When the animation ends -> Reset  all the positions */
            posGen = 0;
            int i = 0;
            while(i < neighsIndex.size()){
                neighPosGen.set(i, 0);
                i++;
            }
        }
        return posPoints.get(++posGen);
    }

    private LatLng getNextNeighPosition(String id){

        if(neighPosGen.get(getIndex(id)) == neighPos.get(getIndex(id)).size()-1) {
            return neighPos.get(getIndex(id)).get(neighPosGen.get(getIndex(id)));
        }

        LatLng pos = neighPos.get(getIndex(id)).get(neighPosGen.get(getIndex(id)));
        neighPosGen.set(getIndex(id), neighPosGen.get(getIndex(id)) +1);
        return pos;
    }

    private Double getNextCourse(){
        if(courseGen == coursePoints.size()-1)
            courseGen = 0;
        return coursePoints.get(++courseGen);
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