package com.example.screen;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ReceiveGpsData extends AsyncTask<Void, String,Void> {

    private static final String TAG = "Debug";

    private int dstPort;
    private int recvPort;
    private String dstIp;
    private NodesData nodesData;
    private boolean newData = false;
    private boolean newMarker = false;
    private String newDataId = null;

    ReceiveGpsData(int RecvPort, int DstPort, String DstIP,NodesData nData){
        recvPort = RecvPort;
        dstPort = DstPort;
        dstIp = DstIP;
        nodesData = nData;
    }

    @Override
    protected Void doInBackground(Void... params) {

        DatagramSocket datagramSocket = null;
        DatagramPacket datagramPacket;
        byte[] message = new byte[1500];

        String text;
        String[] separated;
        try {
            datagramSocket = new DatagramSocket(recvPort);

            InetAddress dstAddress = InetAddress.getByName(dstIp);

            datagramPacket = new DatagramPacket(message, message.length, dstAddress, dstPort);
            datagramSocket.send(datagramPacket);
            Log.i(TAG,"Message sent");

            datagramPacket = new DatagramPacket(message,message.length);

            while (true){
                datagramSocket.receive(datagramPacket);
                /** Log.i(TAG, "\nDatagram Packet Received..."); */

                text = new String(message, 0, datagramPacket.getLength());
                /**Log.i(TAG, "\nMessage Received: "+text); */

                separated = text.split(" ");

                this.publishProgress(
                        separated[0],
                        separated[1],
                        separated[2],
                        separated[3],
                        separated[4]);
            }
       } catch (UnknownHostException e) {
            e.printStackTrace();
            Log.i(TAG, "UnknownHostException: " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"IOException: " + e.toString());
        } finally {
            if (datagramSocket != null)
                datagramSocket.close();
        }
        return null;
    }

    public boolean isNewDataAvailable(){
        return newData;
    }

    public String getNewDataId(){
        return newDataId;
    }

    public void resetNewDataInfo(){
        newData = false;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);

        if( nodesData.updateNodesInfo(new GpsData(values[0], values[1], values[2], values[3], values[4])) != 0){
            newData = true;
            newDataId = values[0];
        }
    }
}