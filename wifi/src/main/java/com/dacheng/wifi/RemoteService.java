package com.dacheng.wifi;

/**
 * Created by dacheng on 2017/6/6.
 */

public interface RemoteService {

    public void connect();

    public void stop();

    public boolean isConnected();

    public void send(ConnectPacket gamePacket);

    public ConnectPacket receive();
}
