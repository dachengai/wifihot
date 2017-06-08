package com.dacheng.wifi;

import java.io.Serializable;

/**
 * Created by dacheng on 2017/6/6.
 */

public class ConnectPacket implements Serializable {
    public int choiseIndex;

    public ConnectPacket(int index){
        choiseIndex = index;
    }
}
