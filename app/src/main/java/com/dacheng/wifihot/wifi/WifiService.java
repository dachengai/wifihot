package com.dacheng.wifihot.wifi;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

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

    protected final int helloMessage = 100;

    protected Socket socket = null;

    private boolean isConnected = false;
    private LinkedList<ConnectPacket> toSendQueue;
    private LinkedList<ConnectPacket> receivedQueue;
    private SendingThread sendingThread;
    private ReceivingThread receivingThread;
    private ConnectingThread connectingThread;

    public WifiService() {
        toSendQueue = new LinkedList<ConnectPacket>();
        receivedQueue = new LinkedList<ConnectPacket>();

        sendingThread = new SendingThread();
        receivingThread = new ReceivingThread();
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
        System.out.println("Run send");
        toSendQueue.offer(gamePacket);
    }

    public ConnectPacket receive() {
        System.out.println("Run receive");
        if(!receivedQueue.isEmpty()){
            System.out.println("receive something");
            return receivedQueue.poll();
        }
        else{
            System.out.println("receive null");
            return null;
        }

    }

    public static String getHostAdress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        int ip = info.getIpAddress();
        @SuppressWarnings("deprecation")
        String ipText = Formatter.formatIpAddress(ip);

        return ipText;
    }

    class SendingThread extends Thread {
        private boolean isAlive = false;
        private OutputStream outputStream;
        private ObjectOutputStream objectOutputStream;
        private OutputStreamWriter outputStreamWriter;

        @Override
        public void run() {
            isAlive = true;
            try {
                System.out.println("start SendingThread");
                while(!isConnected && isAlive)
                    Thread.sleep(250);

                System.out.println("SendingThread: Sending connected");

                if(isAlive) {
                    outputStream = socket.getOutputStream();
                    objectOutputStream = new ObjectOutputStream(outputStream);
                }
                ConnectPacket packet;

                while(isAlive) {
                    System.out.println("SendingThread: try send");

                    if(!toSendQueue.isEmpty()) {
                        System.out.println("SendingThread: Queue length:" + toSendQueue.size());
                        packet = toSendQueue.poll();
                        objectOutputStream.writeObject(packet);
                        objectOutputStream.flush();
                        System.out.println("SendingThread: Sended! choise:" + packet.choiseIndex + "Queue length:" + toSendQueue.size());

                    }

                    Thread.sleep(500);
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
        private InputStreamReader inputStreamReader;

        @Override
        public void run() {
            isAlive = true;
            try {
                System.out.println("start ReceivingThread");
                while(!isConnected && isAlive)
                    Thread.sleep(250);

                System.out.println("ReceivingThread: Receiving connected");

                if(isAlive) {
                    System.out.println("ReceivingThread: getInputStream");
                    inputStream = socket.getInputStream();
                    System.out.println("ReceivingThread: create objectInputStream");
                    objectInputStream = new ObjectInputStream(inputStream);
                    System.out.println("ReceivingThread: created objectInputStream");
                }
                ConnectPacket packet = null;

                while(isAlive) {

                    System.out.println("ReceivingThread: try readObject");
                    packet = (ConnectPacket) objectInputStream.readObject();
                    System.out.println("ReceivingThread: readed object");

                    if(packet != null){
                        receivedQueue.offer(packet);
                        System.out.println("ReceivingThread: received! choise:" + packet.choiseIndex + " Queuelength:" + receivedQueue.size());
                    }

                    Thread.sleep(500);
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

                        System.out.println("ConnectingThread: send hello message");
                        output.write(helloMessage);	// send your DEVICE_TYPE

                        System.out.println("ConnectingThread: recieve hello message");
                        int hello = input.read();
                        if(hello == helloMessage) {
                            System.out.println("ConnectingThread: hello message is correct");

                            isAlive = true;
                        }else{
                            System.out.println("ConnectingThread: hello message is wrong:" + hello);
                            System.out.println("ConnectingThread: recieve something:" + input.read());
                            System.out.println("ConnectingThread: recieve something:" + input.read());
                            System.out.println("ConnectingThread: recieve something:" + input.read());
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

                        System.out.println("ConnectingThread: IOException:" + e1.toString());
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

}
