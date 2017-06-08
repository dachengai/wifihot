package com.dacheng.wifi;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Created by dacheng on 2017/6/6.
 */

public abstract class WifiService implements RemoteService {
    private final static String TAG = "WifiService";
    protected final int helloMessage = 100;

    protected Socket socket = null;

    private boolean isConnected = false;
    private LinkedList<ConnectPacket> toSendQueue;
    private LinkedList<ConnectPacket> receivedQueue;
    private SendingThread sendingThread;
    private ReceivingThread receivingThread;
    private ConnectingThread connectingThread;

    public WifiService(ReceiveListener listener) {
        toSendQueue = new LinkedList<ConnectPacket>();
        receivedQueue = new LinkedList<ConnectPacket>();

        sendingThread = new SendingThread();
        receivingThread = new ReceivingThread(listener);
        connectingThread = new ConnectingThread();
    }

    abstract public Socket connectSpecific();

    abstract public void cancelSpecific();

    public void connect() {
        if(!connectingThread.isAlive()) connectingThread.start();
    }

    public void stop() {
        while(sendingThread.isAlive() || receivingThread.isAlive() || connectingThread.isAlive()) {
            try {
                Thread.sleep(550);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(sendingThread != null && sendingThread.isAlive()) sendingThread.cancel();
            if(receivingThread != null && receivingThread.isAlive()) receivingThread.cancel();
            if(connectingThread != null && connectingThread.isAlive()) connectingThread.cancel();
        }
    }

    public boolean isConnected() {
        if(socket != null && this.isConnected)
            return socket.isConnected();
        else
            return false;
    }

    public void send(ConnectPacket gamePacket) {
        Log.e(TAG,"Run send");
        toSendQueue.offer(gamePacket);
    }

    public ConnectPacket receive() {
        if(!receivedQueue.isEmpty()){
            Log.e(TAG,"receive something");
            return receivedQueue.poll();
        }
        else{
            Log.e(TAG,"receive null");
            return null;
        }

    }

    /**
     * 获取开启便携热点后自身热点IP地址
     * @param context
     * @return
     */
    public static String getHotspotLocalIpAddress(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifimanager.getDhcpInfo();
        if(dhcpInfo != null) {
            int address = dhcpInfo.serverAddress;
            return ((address & 0xFF)
                    + "." + ((address >> 8) & 0xFF)
                    + "." + ((address >> 16) & 0xFF)
                    + "." + ((address >> 24) & 0xFF));
        }
        return null;
    }

    class SendingThread extends Thread {
        private boolean isAlive = false;
        private OutputStream outputStream;
        private ObjectOutputStream objectOutputStream;

        @Override
        public void run() {
            isAlive = true;
            try {
                Log.e(TAG,"start SendingThread");
                while(!isConnected && isAlive)
                    Thread.sleep(250);

                Log.e(TAG,"SendingThread: Sending connected");

                if(isAlive) {
                    outputStream = socket.getOutputStream();
                    objectOutputStream = new ObjectOutputStream(outputStream);
                }
                ConnectPacket packet;

                while(isAlive) {
                    Log.e(TAG,"SendingThread: try send");

                    if(!toSendQueue.isEmpty()) {
                        Log.e(TAG,"SendingThread: Queue length:" + toSendQueue.size());
                        packet = toSendQueue.poll();
                        objectOutputStream.writeObject(packet);
                        objectOutputStream.flush();
                        Log.e(TAG,"SendingThread: Sended! choise:" + packet.choiseIndex + "Queue length:" + toSendQueue.size());

                    }

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                isAlive = false;
            }
        }

        public void cancel() {
            isAlive = false;
            try {
                if(outputStream != null) outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ReceivingThread extends Thread {
        private boolean isAlive = false;
        private InputStream inputStream;
        private ObjectInputStream objectInputStream;
        private ReceiveListener mListener;

        public ReceivingThread(ReceiveListener listener){
            mListener = listener;
        }
        @Override
        public void run() {
            isAlive = true;
            try {
                Log.e(TAG,"start ReceivingThread");
                while(!isConnected && isAlive)
                    Thread.sleep(250);

                Log.e(TAG,"ReceivingThread: Receiving connected");

                if(isAlive) {
                    Log.e(TAG,"ReceivingThread: getInputStream");
                    inputStream = socket.getInputStream();
                    Log.e(TAG,"ReceivingThread: create objectInputStream");
                    objectInputStream = new ObjectInputStream(inputStream);
                    Log.e(TAG,"ReceivingThread: created objectInputStream");
                }
                ConnectPacket packet = null;

                while(isAlive) {

                    Log.e(TAG,"ReceivingThread: try readObject");
                    packet = (ConnectPacket) objectInputStream.readObject();
                    Log.e(TAG,"ReceivingThread: readed object");

                    if(packet != null){
                        if (mListener!= null){//回调
                            mListener.onReceive(packet);
                        }else {//放队列里
                            receivedQueue.offer(packet);
                        }
                        Log.e(TAG,"ReceivingThread: received! choise:" + packet.choiseIndex + " Queuelength:" + receivedQueue.size());
                    }

                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                isAlive = false;
            }

        }

        public void cancel() {
            isAlive = false;
            try {
                if(inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ConnectingThread extends Thread {
        private boolean isAlive = false;

        @Override
        public void run() {
            isAlive = true;
            while(!isConnected && isAlive) {
                socket = connectSpecific();	// can block

                if(socket != null) {
                    try {
                        InputStream input = socket.getInputStream();
                        OutputStream output = socket.getOutputStream();

                        Log.e(TAG,"ConnectingThread: send hello message");
                        output.write(helloMessage);	// send your DEVICE_TYPE

                        Log.e(TAG,"ConnectingThread: recieve hello message");
                        int hello = input.read();
                        if(hello == helloMessage) {
                            Log.e(TAG,"ConnectingThread: hello message is correct");

                            isAlive = true;
                        }else{
                            Log.e(TAG,"ConnectingThread: hello message is wrong:" + hello);
                            Log.e(TAG,"ConnectingThread: recieve something:" + input.read());
                            Log.e(TAG,"ConnectingThread: recieve something:" + input.read());
                            Log.e(TAG,"ConnectingThread: recieve something:" + input.read());
                            socket.close();
                            socket = null;

                            isAlive = false;
                        }

                    } catch (IOException e1) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        socket = null;
                        e1.printStackTrace();

                        isAlive = false;

                        Log.e(TAG,"ConnectingThread: IOException:" + e1.toString());
                    }
                }

                isConnected = socket != null;

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(!sendingThread.isAlive() && isAlive) sendingThread.start();
            if(!receivingThread.isAlive() && isAlive) receivingThread.start();
        }

        public void cancel() {
            cancelSpecific();
            isAlive = false;
        }
    }


    public interface ReceiveListener{
        void onReceive(ConnectPacket packet);
    }
}
