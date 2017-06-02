package com.example.screen;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ReceiveGpsData extends AsyncTask<Void, String,Void> {

    private static final String TAG = "debug";
    private static final int TIMEOUT_MS = 5000;

    private int dstPort;
    private int recvPort;
    private String dstIp;
    private NodesData nodesData;
    private DatagramSocket datagramSocket = null;
    private DatagramPacket datagramPacket;
    private byte[] message = null;


    ReceiveGpsData(int RecvPort, int DstPort, String DstIP, NodesData nData){
        recvPort = RecvPort;
        dstPort = DstPort;
        dstIp = DstIP;
        nodesData = nData;
    }

    @Override
    protected Void doInBackground(Void... params) {

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
                    continue;
                }

                text = new String(message, 0, datagramPacket.getLength());
                /** Log.i(TAG, "\nMessage Received: "+text); */

                separated = text.split(" ");

                this.publishProgress(
                        separated[0],
                        separated[1],
                        separated[2],
                        separated[3],
                        separated[4]);

                SystemClock.sleep(1);
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
        nodesData.updateNodesData(new GpsData(values[0], values[1], values[2], values[3], values[4]));
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