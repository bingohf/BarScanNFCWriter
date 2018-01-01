package com.ledway.scanmaster.domain;

import android.content.Context;
import android.provider.Settings;
import com.ledway.scanmaster.interfaces.IDGenerator;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by togb on 2017/3/4.
 */

public class TimeIDGenerator implements IDGenerator {
  private static final String DATA_FORMAT ="yyyyMMdd'T'HHmmss.S";
  private static final String ID_FORMAT ="%s-%s-%s";
  private final Context mContext;
  private String mDeviceId;
  private String mDeviceName;
  private SimpleDateFormat mSimpleDateFormater;

  public TimeIDGenerator(Context context){
    this.mContext = context;
    prepare();
  }

  private void prepare() {
    mDeviceId = Settings.Secure.getString(mContext.getContentResolver(),
        Settings.Secure.ANDROID_ID);
    mDeviceName = android.os.Build.MODEL;
    mSimpleDateFormater = new SimpleDateFormat(DATA_FORMAT);
  }

  @Override public String genID() {
    return String.format(ID_FORMAT, mDeviceName, mDeviceId, mSimpleDateFormater.format(new Date()));
  }
}
