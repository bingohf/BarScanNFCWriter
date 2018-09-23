package com.ledway.btprinter.network.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import com.ledway.btprinter.MApp;
import com.ledway.scanmaster.AppConstants;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by togb on 2018/9/23.
 */

public class BaseRequest {
  public final String line;
  public final String reader;
  public final String MyTaxNo;
  public final String pdaGuid;

  public BaseRequest() {
    MApp context = MApp.getApplication();
    SharedPreferences sp =
        context.getSharedPreferences(AppConstants.SP_NAME, Context.MODE_PRIVATE);
    line = sp.getString(AppConstants.SP_LINE, "01");
    reader = sp.getString(AppConstants.SP_READER, "01");
    MyTaxNo = sp.getString("MyTaxNo", "");

    String mDeviceId =
        Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    String mDeviceName = Build.MODEL;
    final String DATA_FORMAT ="yyyyMMdd'T'HHmmss.S";
    final String ID_FORMAT ="%s-%s-LEDWAY-%s";
    SimpleDateFormat mSimpleDateFormater = new SimpleDateFormat(DATA_FORMAT);
    pdaGuid=  String.format(ID_FORMAT,  mDeviceId,mDeviceName, mSimpleDateFormater.format(new Date()));
  }
}
