package com.example.screen;

import android.util.Log;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class TrafficSignalTable {

    private static final String TAG = "debugAPP";
    private static final int NEW_NODE = 0;
    private static final int EXISTING_NODE_NOT_UPDATED = 1;
    private static final int EXISTING_NODE_UPDATED = 2;
    private static final int TIME_TO_LIVE = 10000;      // In ms

    private List<String> tableIndex = null;
    /**
     * Used to list all the entries on the table
     */
    public Hashtable<String, TrafficSignalData> trafficSignalDataHashtable = null;

    TrafficSignalTable() {
        trafficSignalDataHashtable = new Hashtable<String, TrafficSignalData>();
        tableIndex = new ArrayList<>();
    }

    public int updateTrafficSignalTable(TrafficSignalData trafficSignalData) {

        String id = trafficSignalData.getId();
        if (contains(id)) {
            updateTTL(id);
            if (!getTrafficSignalData(id).equals(trafficSignalData)) {
                update(trafficSignalData);
                return EXISTING_NODE_UPDATED;
            } else
                return EXISTING_NODE_NOT_UPDATED;
        } else {
            add(trafficSignalData);
            return NEW_NODE;
        }
    }

    public void getNodeInfo(String id) {
        Log.i(TAG, "Id: " + trafficSignalDataHashtable.get(id).getId()
                + " Lon: " + trafficSignalDataHashtable.get(id).getLon()
                + " Lat: " + trafficSignalDataHashtable.get(id).getLat()
                + " Course: " + trafficSignalDataHashtable.get(id).getCourse());
    }

    public void getAllNodesInfo() {
        int i = 0;
        while (i < trafficSignalDataHashtable.size()) {
            Log.i(TAG, "index: " + i);
            Log.i(TAG, "Id: " + trafficSignalDataHashtable.get(getKey(i)).getId()
                    + " Lon: " + trafficSignalDataHashtable.get(getKey(i)).getLon()
                    + " Lat: " + trafficSignalDataHashtable.get(getKey(i)).getLat()
                    + " Course: " + trafficSignalDataHashtable.get(getKey(i)).getCourse());
            i++;
        }
    }

    public TrafficSignalData getTrafficSignalData(String id) {
        return trafficSignalDataHashtable.get(id);
    }

    public List<TrafficSignalData> getTrafficSignalsData() {
        List<TrafficSignalData> trafficSignalDataList = new ArrayList<>();
        int i = 0;
        while (i < tableIndex.size()) {
            trafficSignalDataList.add(getTrafficSignalData(getKey(i)));
            i++;
        }
        return trafficSignalDataList;
    }

    private void add(TrafficSignalData trafficSignalData) {
        trafficSignalData.increaseTTl(1000);
        tableIndex.add(trafficSignalData.getId());
        trafficSignalDataHashtable.put(trafficSignalData.getId(), trafficSignalData);
    }

    private void update(TrafficSignalData trafficSignalData) {
        trafficSignalDataHashtable.put(trafficSignalData.getId(), trafficSignalData);
    }

    private void updateTTL(String id){
        TrafficSignalData trafficSignalData = trafficSignalDataHashtable.get(id);
        trafficSignalData.setTTL(TIME_TO_LIVE);
        trafficSignalDataHashtable.put(id, trafficSignalData);
    }

    private String getKey(int i) {
        return tableIndex.get(i);
    }

    private boolean contains(String id) {
        return trafficSignalDataHashtable.containsKey(id);
    }

    private void remove(String id) {
        trafficSignalDataHashtable.remove(id);
        tableIndex.remove(id);
    }

    public void updateTime() {
        int i = 0;
        TrafficSignalData trafficSignalData;
        List<String> removeList = new ArrayList<>();

        while (i < tableIndex.size()) {
            trafficSignalData = getTrafficSignalData(getKey(i));
            trafficSignalData.decreaseTTL(1000);

            trafficSignalDataHashtable.put(trafficSignalData.getId(), trafficSignalData);
            i++;
        }
    }
}
