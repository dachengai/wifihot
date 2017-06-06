package com.dacheng.wifihot;

import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.dacheng.wifihot.wifi.ConnectPacket;
import com.dacheng.wifihot.wifi.WifiApManager;
import com.dacheng.wifihot.wifi.WifiDevice;
import com.dacheng.wifihot.wifi.WifiHostService;
import com.dacheng.wifihot.wifi.WifiScanListener;
import com.dacheng.wifihot.wifi.WifiService;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    WifiApManager wifiApManager;
    TextView textView1;
    private WifiService mHostService;
    private boolean isActive = true;

    private final int EVENT_FETCH_PACKAGE = 8;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView1 = (TextView) findViewById(R.id.text1);
        wifiApManager = new WifiApManager(this);
        //设置wifi信息
        wifiApManager.setWifiApConfiguration(getWifiConfigutaion("dacheng","12345678"));
        //开启热点
        wifiApManager.setWifiApEnabled(null, true);
        //开启socket服务 发送/接收数据
        connect(new WifiHostService(),10);

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Get Clients");
        menu.add(0, 1, 0, "Open AP");
        menu.add(0, 2, 0, "Close AP");
        menu.add(0, 3, 0, "get WifiApConfiguration");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:

                break;
            case 1:
                wifiApManager.setWifiApEnabled(null, true);
                break;
            case 2:
                wifiApManager.setWifiApEnabled(null, false);
                break;
            case 3:
                String info = wifiApManager.getWifiApConfiguration().toString();
                Log.e(TAG,"info = "+info);
                textView1.setText(info);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActive = false;
    }

    public static WifiConfiguration getWifiConfigutaion(String SSID, String password){
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = SSID;
        wc.preSharedKey = password;
        wc.hiddenSSID = false;
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        return wc;
    }



    protected void connect(WifiService service, final int timeoutSecond) {
        mHostService = service;
        Thread thread = new Thread(new Runnable() {

            public void run() {
                mHostService.connect();

                int trialsCounter = 0;
                int COUNTER_MAX = timeoutSecond * 2;
                while(!mHostService.isConnected() && trialsCounter < COUNTER_MAX && isActive) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    trialsCounter++;
                }

                if(trialsCounter < COUNTER_MAX && isActive){
                    // 开启handler 线程处理数据
                    new WorkThread("workthread").start();
                } else if(isActive){
                    mHostService.stop();
                    finish();
                }
            }
        });
        thread.start();
    }

    private Handler mMediaPlayerHandler;
    private class WorkThread extends HandlerThread {

        public WorkThread(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            mMediaPlayerHandler = new Handler(this.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case EVENT_FETCH_PACKAGE:
                            ConnectPacket packet = mHostService.receive();
                            if (packet != null) {
                                Log.e(TAG,"has received package");

                            }else {
                                Log.e(TAG,"no package wait 100ms");
                                this.sendEmptyMessageDelayed(EVENT_FETCH_PACKAGE,100);
                            }
                            break;
                        default:
                            break;
                    }
                    super.handleMessage(msg);
                }
            };
            mMediaPlayerHandler.sendEmptyMessage(EVENT_FETCH_PACKAGE);
        }
    }
}
