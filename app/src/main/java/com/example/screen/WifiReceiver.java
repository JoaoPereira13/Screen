package com.example.screen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;


public class WifiReceiver extends BroadcastReceiver {

    private static String TAG = "debug";
    private ConnectionManagement CM;

    protected WifiReceiver(ConnectionManagement CM){
        super();
        this.CM = CM;
    }

    @Override
    public void onReceive(Context arg0, Intent arg1){
        String action = arg1.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            Log.i(TAG,"Scan results available...");
            CM.results = CM.wifi.getScanResults();

            CM.mAdapter.clear();

            for (ScanResult result:CM.results) {
                CM.mAdapter.add(result.SSID + " : " + result.BSSID + " " + result.level);
            }
            CM.listView.setAdapter(CM.mAdapter);
        }
    }
}
