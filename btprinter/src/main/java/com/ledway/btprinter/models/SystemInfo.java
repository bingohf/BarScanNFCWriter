package com.ledway.btprinter.models;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import com.ledway.btprinter.MApp;

/**
 * Created by togb on 2016/6/5.
 */
public class SystemInfo {
  private final String macAddress;
  private final String deviceId;

  public SystemInfo(Context context) {
    WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    WifiInfo info = manager.getConnectionInfo();
    macAddress = info.getMacAddress();
    deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
  }

  public String getMacAddress() {
    return macAddress;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getBusinessCard() {
    String stirng = MApp.getApplication()
        .getSharedPreferences("qrcode", Context.MODE_PRIVATE)
        .getString("qrcode", "");
    String[] ss = stirng.split("\\r|\\n");
    StringBuilder sb = new StringBuilder();
    for(int i =0;i < ss.length && i < 2; ++i){
      if(i > 0){
        sb.append("\r\n");
      }
      sb.append(ss[i]);
    }
    return  sb.toString();

  }
}
