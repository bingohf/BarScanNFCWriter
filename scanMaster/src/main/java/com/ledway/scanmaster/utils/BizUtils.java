package com.ledway.scanmaster.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import com.ledway.scanmaster.AppConstants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by togb on 2018/2/14.
 */

public class BizUtils {
  public static String getMyTaxNo(Context context){

    final String DATA_FORMAT ="yyyyMMdd'T'HHmmss.S";
    final String ID_FORMAT ="%s-%s-LEDWAY-%s";
    SharedPreferences sp =
        context.getSharedPreferences(AppConstants.SP_NAME, Context.MODE_PRIVATE);
    String taxNo = sp.getString("MyTaxNo", "");
    if(!TextUtils.isEmpty(taxNo)) {
      return taxNo;
    }else {
      String mDeviceId =
          Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
      String mDeviceName = Build.MODEL;
      SimpleDateFormat mSimpleDateFormater = new SimpleDateFormat(DATA_FORMAT);
      String temp = String.format(ID_FORMAT,  mDeviceId,mDeviceName, mSimpleDateFormater.format(new Date()));
      return temp+ "~" + getLanguage();
    }
  }
  static String getLanguage() {
    Locale locale = Locale.getDefault();
    return locale.getLanguage() + "_" + locale.getCountry();
  }
}
