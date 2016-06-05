package com.ledway.btprinter.models;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by togb on 2016/6/5.
 */
public class SystemInfo {
  private final String macAddress;

  public SystemInfo(Context context){
    WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    WifiInfo info = manager.getConnectionInfo();
    macAddress = info.getMacAddress();
  }

  public String getMacAddress(){
    return  macAddress;
  }
}
