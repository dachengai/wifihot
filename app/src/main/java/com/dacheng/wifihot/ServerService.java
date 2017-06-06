package com.dacheng.wifihot;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.support.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by dacheng on 2017/6/6.
 */

public class ServerService extends CMIntentService {

    private final static String TAG = "ServerService";
    private static final int NOTIFICATION_ID = 1;

    private final int port = 7850;
    public ServerService() {
        super("PrService");
        setIntentRedelivery(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    protected boolean onHandleIntent(@Nullable Message msg) {


        ServerSocket welcomeSocket = null;
        Socket socket = null;
        try {
            welcomeSocket = new ServerSocket(port);
            while(true )
            {
                socket = welcomeSocket.accept();
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String cmd = "test" + br.readLine() + "test";
                socket.close();

            }


        } catch (IOException e) {
            e.getMessage();

        }
        catch(Exception e)
        {
           e.getMessage();

        }
        return true;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 启动该service
     */
    public static void startServerServcie(Context context) {
        if (null == context)
            return;
        Intent aIntent = new Intent();
        aIntent.setClass(context, ServerService.class);
        context.startService(aIntent);
    }


}
