package com.example.screen;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

public class ReceiveData extends AsyncTask<Void, String,Void> {

    private static final String TAG = "debug";
    private static final int TIMEOUT_MS = 5000;
    ToastHandler mToastHandler = new ToastHandler(getApplicationContext());

    private int dstPort;
    private int recvPort;
    private String dstIp;
    private VehicularTable vehicularTable;
    private TrafficSignalTable trafficSignalTable;
    private DatagramSocket datagramSocket = null;
    private DatagramPacket datagramPacket;
    private byte[] message = null;



    ReceiveData(int RecvPort, int DstPort, String DstIP, VehicularTable vTable, TrafficSignalTable tsTable){
        recvPort = RecvPort;
        dstPort = DstPort;
        dstIp = DstIP;
        vehicularTable = vTable;
        trafficSignalTable = tsTable;
    }

    @Override
    protected Void doInBackground(Void... params) {
        /**
         * Thread running a Loop function -> runs until this thread is cancelled on the
         * DisplayMap activity
         *
         * Receives the vehicular data
         * Stores this data on vehicularTable
         *
         * Receives the traffic signal data
         * If the socket times out during the reception, send a request packet to the OBU
         * to restart this process
         */

        message = new byte[1500];

        String text;
        String[] separated;
        try {
            datagramSocket = new DatagramSocket(recvPort);
            datagramSocket.setSoTimeout(TIMEOUT_MS);

            InetAddress dstAddress = InetAddress.getByName(dstIp);

            datagramPacket = new DatagramPacket(message, message.length, dstAddress, dstPort);
            datagramSocket.send(datagramPacket);
            Log.i(TAG,"Requesting GPS Data From: "+dstIp+":"+dstPort);

            datagramPacket = new DatagramPacket(message,message.length);

            while (true){

                if (isCancelled()) break;

                try {
                    /** Log.i(TAG, "Trying to receive..."); */
                    datagramSocket.receive(datagramPacket);
                    /** Log.i(TAG, "\nDatagram Packet Received..."); */
                }catch (SocketTimeoutException e) {
                    /** timeout exception */
                    Log.i(TAG, "IOException: " + e.toString());
                    sendRequestPacket();
                    mToastHandler.showToast("Connection Unavailable. Trying to reconnect...", Toast.LENGTH_SHORT);
                    continue;
                }

                text = new String(message, 0, datagramPacket.getLength());
                /** Log.i(TAG, "\nPacket Received: "+text);*/

                separated = text.split(" ");

                this.publishProgress(
                        separated[0],
                        separated[1],
                        separated[2],
                        separated[3],
                        separated[4]);

                SystemClock.sleep(1);   /** ms */
            }
       } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.i(TAG, "UnknownHostException: " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "IOException: " + e.toString());
        } finally {
            if (datagramSocket != null)
                datagramSocket.close();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        /** IF traffic signal data - format: [ID Lon Lat Course Message] */
        if(values[4].contains("TS_")) {
            trafficSignalTable.updateTrafficSignalTable(new TrafficSignalData(values[0], values[1], values[2], values[3], values[4]));
            //Log.i(TAG,"\nReceived TS Packet");
        }
        /** IF vehicular data - format: [ID Lon Lat Speed Course] */
        else{
            vehicularTable.updateVehicularTable(new VehicularData(values[0], values[1], values[2], values[3], values[4]));
        }
    }

    private void sendRequestPacket() {
        try {
            InetAddress dstAddress = InetAddress.getByName(dstIp);

            datagramPacket = new DatagramPacket(message, message.length, dstAddress, dstPort);
            datagramSocket.send(datagramPacket);
            Log.i(TAG, "Requesting GPS Data From: " + dstIp + ":" + dstPort);

        } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.i(TAG, "UnknownHostException: " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "IOException: " + e.toString());
        }
    }
}