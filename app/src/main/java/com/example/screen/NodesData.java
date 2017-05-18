package com.example.screen;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;


public class NodesData {

    private static final String TAG = "debug";
    private ArrayList<Integer> lastUpdateMs = null;
    private List<GpsData> nodesGps = null;
    private String userId = null;
    private boolean userDataAvailable;

    public List<String> nodesIndex = null;

    NodesData() {
        lastUpdateMs = new ArrayList<>();
        nodesIndex = new ArrayList<>();
        nodesGps = new ArrayList<>();
        userDataAvailable = false;
    }


    private void updateTime() {
        /**

         for all elements {
         add +XX MS to the lastUpdateMS;
         if ( element.lastUpdateMs > threshold)
         remove(element)
         }
         */

    }

    private void add(GpsData gps) {
        nodesIndex.add(gps.getId());
        nodesGps.add(gps);
        if(gps.getId().equals(userId))
            userDataAvailable = true;
    }

    private boolean contains(String id) {
        return nodesIndex.contains(id);
    }

    private int getIndex(String id) {
        return nodesIndex.indexOf(id);
    }

    private void setGps(String id, GpsData gps) {
        nodesGps.set(getIndex(id), gps);
    }

    private GpsData getGps(String id) {
        return nodesGps.get(getIndex(id));
    }

    private void remove(String id) {
        nodesGps.remove(getIndex(id));
        nodesIndex.remove(id);
    }

    public int updateNodesInfo(GpsData gps) {

        //updateTime();
        String id = gps.getId();

        if (contains(id)) {                     /** Node already exists */
            if (!getGps(id).equals(gps)) {      /** Node is not updated */
                setGps(id, gps);
                return 1;
            } else                              /** Node data is updated */
                return 0;
        } else {
            add(gps);
            return 2;                           /** New Node Added */
        }
    }

    public GpsData getUserData(){
        return nodesGps.get(getIndex(userId));
    }

    public GpsData getNeighData(String id){
        return nodesGps.get(getIndex(id));
    }

    public void getAllNodesInfo() {
        int i = 0;
        while (i < nodesGps.size()) {
            Log.i(TAG, "index: " + i);
            Log.i(TAG,
                    "Id: " + nodesGps.get(i).getId()
                            + " Lon: " + nodesGps.get(i).getLon()
                            + " Lat: " + nodesGps.get(i).getLat()
                            + " Speed: " + nodesGps.get(i).getSpeed()
                            + " Course: " + nodesGps.get(i).getCourse());
            i++;
        }
    }

    public void getNodeInfo(String id){
        int i = getIndex(id);
        Log.i(TAG,
                "Id: " + nodesGps.get(i).getId()
                        + " Lon: " + nodesGps.get(i).getLon()
                        + " Lat: " + nodesGps.get(i).getLat()
                        + " Speed: " + nodesGps.get(i).getSpeed()
                        + " Course: " + nodesGps.get(i).getCourse());
    }

    public void getUserInfo(){
        int i = getIndex(userId);
        Log.i(TAG,
                "Id: " + nodesGps.get(i).getId()
                        + " Lon: " + nodesGps.get(i).getLon()
                        + " Lat: " + nodesGps.get(i).getLat()
                        + " Speed: " + nodesGps.get(i).getSpeed()
                        + " Course: " + nodesGps.get(i).getCourse());
    }

    public void setUserId(String id){
        userId = id;
    }

    public boolean isUserDataAvailable(){
        return userDataAvailable;
    }

    public String getUserId(){
        return userId;
    }
}