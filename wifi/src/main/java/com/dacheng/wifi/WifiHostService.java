package com.dacheng.wifi;

/**
 * Created by dacheng on 2017/6/6.
 */

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WifiHostService extends WifiService {
    private final static String TAG = "WifiHostService";
    private ServerSocket serverSocket;

    public WifiHostService() {
        super(null);
    }

    public WifiHostService(ReceiveListener listener) {
        super(listener);
    }

    @Override
    public Socket connectSpecific() {
        Socket socket = null;
        try {
            Log.e(TAG,"WifiHostService.connectSpecific() start, port:" + WifiConfig.DEFAULT_PORT);
            serverSocket = new ServerSocket(WifiConfig.DEFAULT_PORT);
            socket = serverSocket.accept();
            Log.e(TAG,"WifiHostService.connectSpecific() accepted, address:" + socket.getRemoteSocketAddress().toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

    @Override
    public void cancelSpecific() {
        try {
            if(serverSocket != null)
                serverSocket.close();
            if(super.socket != null)
                super.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}