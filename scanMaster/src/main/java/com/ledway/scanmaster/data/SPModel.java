package com.ledway.scanmaster.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.ledway.scanmaster.AppConstants;
import com.ledway.scanmaster.ApplicationContext;
import javax.inject.Inject;

/**
 * Created by togb on 2017/2/18.
 */

public class SPModel {

  private final SharedPreferences sp;

  @Inject
  public SPModel(@ApplicationContext Context context){
    this.sp = context.getSharedPreferences(AppConstants.SP_NAME, Context.MODE_PRIVATE);

  }

  public SettingSnap loadSetting(){
    SettingSnap settingSnap = new SettingSnap();
    settingSnap.server = sp.getString(AppConstants.SP_SERVER, "www.ledway.com.tw");
    settingSnap.db = sp.getString(AppConstants.SP_DB, "WINUPRFID401");
    settingSnap.line = sp.getString(AppConstants.SP_LINE, "01");
    settingSnap.reader = sp.getString(AppConstants.SP_READER, "01");
    return settingSnap;
  }

  public void save(SettingSnap settingSnap){
    sp.edit().putString(AppConstants.SP_SERVER, settingSnap.server)
        .putString(AppConstants.SP_DB, settingSnap.db)
        .putString(AppConstants.SP_LINE, settingSnap.line)
        .putString(AppConstants.SP_READER, settingSnap.reader).apply();
  }
}
