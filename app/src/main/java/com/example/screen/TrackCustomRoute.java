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

import java.util.ArrayList;
import java.util.List;


public class TrackCustomRoute extends AppCompatActivity implements PermissionsListener {

    private static final String TAG = "debug";
    private static final int refreshPeriodMs = 500;
    private static final int initialDelayMs = 2000;
    private static final int zoomLevel = 16;
    private static final int receivePort = 13892;
    private static final int destinyPort = 13891;
    // TODO Get the ip address of the OBU based on it's SSID
    private static final String destinyIP = "10.6.81.1";
    private static final boolean enableTracking = true;

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap map;
    private LatLng userPosition = null;
    private LatLng neighPosition;

    private MarkerViewOptions userMarker = null;
    private List<MarkerViewOptions> neighMarkers;
    private List<String> neighsIndex = null;
    private NodesData nodesData = new NodesData();
    private ReceiveGpsData receiveGpsData = new ReceiveGpsData(receivePort, destinyPort, destinyIP, nodesData);

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
                            neighPosition = new LatLng(
                                    Double.parseDouble(nodesData.getNeighData(nodesData.nodesIndex.get(i)).getLat()),
                                    Double.parseDouble(nodesData.getNeighData(nodesData.nodesIndex.get(i)).getLon()));
                            refreshNeighPosition(nodesData.nodesIndex.get(i));
                        }
                        else if(nodesData.isUserDataAvailable()){                               /**  User position */
                            userPosition = new LatLng(
                                    Double.parseDouble(nodesData.getUserData().getLat()),
                                    Double.parseDouble(nodesData.getUserData().getLon()));
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

        // TODO Automate the proccess to set the user Id
        nodesData.setUserId("0000");
        receiveGpsData.execute();
        neighMarkers = new ArrayList<>();
        neighsIndex = new ArrayList<>();

        /** Get the user icon */
        IconFactory iconFactory = IconFactory.getInstance(TrackCustomRoute.this);
        Icon icon = iconFactory.fromResource(R.mipmap.purple_round_marker);

        /** Add the user marker in the starting position */
        userPosition = new LatLng(-1,-1);
        userMarker = new MarkerViewOptions()
                .position(userPosition)
                .icon(icon);

        /** Move the camera to the starting position */
        map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(userPosition)
                .zoom(zoomLevel)
                .build()));
        map.addMarker(userMarker);
    }

    private void refreshUserPosition(){
        if(enableTracking) {
            /** Move the camera along the user position */
            map.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                    .target(userPosition)
                    .bearing(Double.parseDouble(nodesData.getUserData().getCourse()))
                    .build()));
            /** Move the user marker */
            map.updateMarker(userMarker
                    .position(userPosition).getMarker());
        }
    }

    private void refreshNeighPosition(String id) {
        /** Create the Neighbor marker */
        if (getIndex(id) == -1) {           // New marker
            addNeighMarker(id);
        }
        else{
            /** Move the Neighbor marker */
            map.updateMarker(neighMarkers.get(getIndex(id))
                    .position(neighPosition).getMarker());
        }
    }

    private void addNeighMarker(String id){
        /** Get the neighbor icon */
        IconFactory iconFactory = IconFactory.getInstance(TrackCustomRoute.this);
        Icon icon = iconFactory.fromResource(R.mipmap.light_blue_round_marker);

        neighMarkers.add(new MarkerViewOptions().position(neighPosition).icon(icon));
        neighsIndex.add(id);
        map.addMarker(neighMarkers.get(getIndex(id)));
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

    private int getIndex(String id) {
        return neighsIndex.indexOf(id);
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
