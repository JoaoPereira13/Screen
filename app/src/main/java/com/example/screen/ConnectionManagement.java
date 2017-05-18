package com.example.screen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class ConnectionManagement extends AppCompatActivity {

    public final static String TAG = "debug";
    private static final int refreshPeriodMs = 1000;
    private static final int initialDelayMs = 0;

    protected List<ScanResult> results;
    protected WifiManager wifi;
    protected ListView listView;
    protected ArrayAdapter<String> mAdapter;

    private BroadcastReceiver receiver;
    private TextView CNtext;
    private Button btn;

    private static Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, refreshPeriodMs);
        }
    };

    private void connect(ScanResult scanResult){
        Log.i(TAG,"Connecting to network: "+scanResult.SSID);

        /** Turn on Wifi */
        if(!wifi.isWifiEnabled())
            wifi.setWifiEnabled(true);

        /** Get the list of configured connections */
        List<WifiConfiguration> list = wifi.getConfiguredNetworks();
        /** Check if the desired connection is already configured */
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + scanResult.SSID + "\"")) {
                /** Connect */
                wifi.disconnect();
                wifi.enableNetwork(i.networkId, true);
                wifi.reconnect();
                break;
            }
        }
        // TODO add the option to configure a new network
        mAdapter.clear();
    }

    private void refresh(){
        /**  Get the current wifi Status */
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        CNtext.setText(info.toString());
    }

    private void register(){
        Log.i(TAG,"Registering the receiver service...");
        /** Register Broadcast Receiver */
        if (receiver == null)
            receiver = new WifiReceiver(this);
        registerReceiver(receiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_management);
        btn = (Button) findViewById(R.id.btn);
        CNtext = (TextView) findViewById(R.id.CN_text);
        listView = (ListView) findViewById(R.id.list_view);

        mAdapter = new ArrayAdapter<>(ConnectionManagement.this,
                android.R.layout.simple_list_item_1);

        handler.postDelayed(runnable, initialDelayMs);

        /** List view Listener */
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,
                                    View view,
                                    int position,
                                    long id) {
                connect(results.get(position));
                Log.i(TAG,"setOnItemClickListener()...");
            }
        });
        /** Button Listener*/
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"setOnClickListener()...");

                if (v.getId() == R.id.btn) {
                    Log.i(TAG,"Start Scanning for networks...");
                    wifi.startScan();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        register();
    }

    @Override
    protected void onStop(){
        super.onStop();
        unregisterReceiver(receiver);
    }
}