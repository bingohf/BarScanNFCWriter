package com.ledway.btprinter.models;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

/**
 * Created by togb on 2016/6/5.
 */
public class SystemInfo {
  private final String macAddress;
  private final String deviceId;
  public SystemInfo(Context context){
    WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    WifiInfo info = manager.getConnectionInfo();
    macAddress = info.getMacAddress();
    deviceId = Settings.Secure.getString(context.getContentResolver(),
        Settings.Secure.ANDROID_ID);
  }

  public String getMacAddress(){
    return  macAddress;
  }

  public String getDeviceId(){
    return deviceId;
  }
}
