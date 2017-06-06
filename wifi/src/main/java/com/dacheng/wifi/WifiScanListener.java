package com.dacheng.wifi;

import java.util.ArrayList;


public interface WifiScanListener {
	
	
	/**
	 * Interface called when the scan method finishes. Network operations should not execute on UI thread  
	 * @param clients ArrayList of {@link WifiDevice}
	 */
	
	public void onFinishScan(ArrayList<WifiDevice> clients);

}
