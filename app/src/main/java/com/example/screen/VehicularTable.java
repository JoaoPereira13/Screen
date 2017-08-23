package com.example.screen;

import android.util.Log;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class VehicularTable {

    private static final String TAG = "debug";
    private static final int NEW_NODE = 0;
    private static final int EXISTING_NODE_NOT_UPDATED = 1;
    private static final int EXISTING_NODE_UPDATED = 2;

    public List<String> tableIndex = null;            /** Used to list all the entries on the table */
    public Hashtable<String,VehicularData> vehicularDataHashtable = null;


    //private ArrayList<Integer> lastUpdateMs = null;
    private String userId = null;
    private boolean userDataAvailable;

    VehicularTable() {
        //lastUpdateMs = new ArrayList<>();
        vehicularDataHashtable = new Hashtable<String, VehicularData>();
        tableIndex = new ArrayList<>();
        userDataAvailable = false;
    }


    public int updateVehicularTable(VehicularData vehicularData) {

        //updateTime();
        String id = vehicularData.getId();

        if (contains(id)) {
            if (!getVehicularData(id).equals(vehicularData)) {
                update(vehicularData);
                return EXISTING_NODE_UPDATED;
            } else
                return EXISTING_NODE_NOT_UPDATED;
        } else {
            add(vehicularData);
            return NEW_NODE;
        }
    }

    public void getNodeInfo(String id){
        Log.i(TAG,
                "Id: " + vehicularDataHashtable.get(id).getId()
                        + " Lon: " + vehicularDataHashtable.get(id).getLon()
                        + " Lat: " + vehicularDataHashtable.get(id).getLat()
                        + " Speed: " + vehicularDataHashtable.get(id).getSpeed()
                        + " Course: " + vehicularDataHashtable.get(id).getCourse());
    }

    public void getUserInfo(){
        Log.i(TAG,
                "Id: " + vehicularDataHashtable.get(userId).getId()
                        + " Lon: " + vehicularDataHashtable.get(userId).getLon()
                        + " Lat: " + vehicularDataHashtable.get(userId).getLat()
                        + " Speed: " + vehicularDataHashtable.get(userId).getSpeed()
                        + " Course: " + vehicularDataHashtable.get(userId).getCourse());
    }

    public void getAllNodesInfo() {
        int i = 0;
        while (i < vehicularDataHashtable.size()) {
            Log.i(TAG, "index: " + i);
            Log.i(TAG,"Id: " + vehicularDataHashtable.get(getKey(i)).getId()
                    + " Lon: " + vehicularDataHashtable.get(getKey(i)).getLon()
                    + " Lat: " + vehicularDataHashtable.get(getKey(i)).getLat()
                    + " Speed: " + vehicularDataHashtable.get(getKey(i)).getSpeed()
                    + " Course: " + vehicularDataHashtable.get(getKey(i)).getCourse());
            i++;
        }
    }

    public VehicularData getVehicularData(String id) {
        return vehicularDataHashtable.get(id);
    }

    public VehicularData getUserData(){
        return vehicularDataHashtable.get(userId);
    }

    public List<VehicularData> getNeighboursData(){
        List<VehicularData> vehicularDataList  = new ArrayList<>();

        int i = 0;
        while(i < tableIndex.size()) {
            if(!getKey(i).equals(getUserId())){                 /** Neighbor position */
                vehicularDataList.add(getVehicularData(getKey(i)));
            }
            i++;
        }
        return vehicularDataList;
    }

    public String getUserId(){
        return userId;
    }

    public void setUserId(String id){
        userId = id;
    }

    public boolean isUserDataAvailable(){
        return userDataAvailable;
    }

    public String getKey(int i){
        return tableIndex.get(i);
    }

    private void add(VehicularData vehicularData) {
        tableIndex.add(vehicularData.getId());
        vehicularDataHashtable.put(vehicularData.getId(), vehicularData);

        if(vehicularData.getId().equals(userId))
            userDataAvailable = true;
    }

    private void update(VehicularData vehicularData) {
        vehicularDataHashtable.put(vehicularData.getId(), vehicularData);
    }

    private boolean contains(String id) {
        return vehicularDataHashtable.containsKey(id);
    }

    private void remove(String id) {
        vehicularDataHashtable.remove(id);
        tableIndex.remove(id);
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

}