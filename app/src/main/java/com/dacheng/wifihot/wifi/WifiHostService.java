package com.dacheng.wifihot.wifi;

/**
 * Created by dacheng on 2017/6/6.
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WifiHostService extends WifiService {

    private ServerSocket serverSocket;

    public WifiHostService() {
        super();
    }

    @Override
    public Socket connectSpecific() {
        Socket socket = null;
        try {
            System.out.println("WifiHostService.connectSpecific() start, port:" + GlobalConfig.port);
            serverSocket = new ServerSocket(GlobalConfig.port);
            socket = serverSocket.accept();

            System.out.println("WifiHostService.connectSpecific() accepted, address:" + socket.getRemoteSocketAddress().toString());

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