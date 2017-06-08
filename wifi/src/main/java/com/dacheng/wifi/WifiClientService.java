package com.dacheng.wifi;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by dacheng on 2017/6/6.
 */

public class WifiClientService extends WifiService {
    private final static String TAG = "WifiClientService";
    private String serverIP;

    public WifiClientService(String serverIP) {
        super(null);
        this.serverIP = serverIP;
    }

    @Override
    public Socket connectSpecific() {
        if(this.serverIP == null)
            return null;

        Socket socket = null;
        try {
            Log.e(TAG," WifiClientService.connectSpecific() start, address:" + this.serverIP + ":" + WifiConfig.DEFAULT_PORT);
            socket = new Socket(this.serverIP, WifiConfig.DEFAULT_PORT);
            Log.e(TAG," WifiClientService.connectSpecific() success, address:" + socket.getRemoteSocketAddress());

        } catch (Exception e) {
            Log.e(TAG,e.toString());
        }

        return socket;
    }

    @Override
    public void cancelSpecific() {
        try {
            if(super.socket != null)
                super.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
