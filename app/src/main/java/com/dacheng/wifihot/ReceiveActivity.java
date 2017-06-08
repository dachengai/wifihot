package com.dacheng.wifihot;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.dacheng.wifi.ConnectPacket;
import com.dacheng.wifi.WifiApManager;
import com.dacheng.wifi.WifiConfig;
import com.dacheng.wifi.WifiHostService;
import com.dacheng.wifi.WifiService;

public class ReceiveActivity extends AppCompatActivity {
    private final static String TAG = "WifiHostService";
    TextView textView1;
    TextView textView2;
    private static WifiService mReceiveService;
    private boolean isActive = true;
    WifiApManager wifiApManager;
    private final int EVENT_CONNECTE_SUCCESS = 8;
    private final int  EVENT_CONNECTE_FAIL = 9;
    private final int  EVENT_PACKAGE = 10;
    private WifiService.ReceiveListener mListener;
    private Handler mHandler  = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_CONNECTE_SUCCESS:
                    textView2.setText("连接成功！");
                    break;
                case EVENT_CONNECTE_FAIL:
                    textView2.setText("连接失败！");
                    break;
                case EVENT_PACKAGE:
                    ConnectPacket packet = (ConnectPacket)msg.obj;
                    textView1.setText("接收到数据： "+ packet.choiseIndex);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView1 = (TextView) findViewById(R.id.text1);
        textView2 = (TextView) findViewById(R.id.text2);
        wifiApManager = new WifiApManager(this);
        //设置wifi信息
        wifiApManager.setWifiApConfiguration(WifiConfig.wifiName, WifiConfig.wifiPwd);

        mListener = new WifiService.ReceiveListener() {
            @Override
            public void onReceive(ConnectPacket packet) {
                if (packet != null) {
                    Message msg = Message.obtain();
                    msg.what = EVENT_PACKAGE;
                    msg.obj = packet;
                    mHandler.sendMessage(msg);
                }
            }
        };

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActive = false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "开启服务");
        menu.add(0, 1, 0, "开启热点");
        menu.add(0, 2, 0, "关闭热点");
        menu.add(0, 3, 0, "关闭服务");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                startConnect();
                break;
            case 1:
                if(!wifiApManager.isWifiApEnabled() && wifiApManager.setWifiApEnabled(null, true)){
                    startConnect();
                }
                break;
            case 2:
                wifiApManager.setWifiApEnabled(null, false);
                break;
            case 3:

                if(mReceiveService != null){
                    isActive = false;
                    mReceiveService.stop();
                    textView2.setText("已断开连接");
                    textView1.setText("接收到数据： 空");
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startConnect(){
        isActive = true;
        //开启socket服务 接收数据
        mReceiveService = new WifiHostService();
        connect(200);
        textView2.setText("连接中..");
    }

    protected void connect( final int timeoutSecond) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                mReceiveService.connect();

                int trialsCounter = 0;
                int COUNTER_MAX = timeoutSecond * 2;
                while(!mReceiveService.isConnected() && trialsCounter < COUNTER_MAX && isActive) {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    trialsCounter++;
                    Log.e(TAG,"try:"+trialsCounter);
                }

                if(trialsCounter < COUNTER_MAX && isActive){
                    // 连接成功，处理数据
                    mHandler.sendEmptyMessage(EVENT_CONNECTE_SUCCESS);
                } else if(isActive){
                    // 连接失败
                    mHandler.sendEmptyMessage(EVENT_CONNECTE_FAIL);
                    mReceiveService.stop();
                }
            }
        });
        thread.start();
    }

}
