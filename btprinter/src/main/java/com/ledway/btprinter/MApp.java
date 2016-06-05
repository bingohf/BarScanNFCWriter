package com.ledway.btprinter;

import android.app.Application;
import com.activeandroid.ActiveAndroid;
import com.facebook.stetho.Stetho;
import com.ledway.btprinter.models.SystemInfo;

/**
 * Created by togb on 2016/5/29.
 */
public class MApp extends Application {
  private static MApp instance;
  private SystemInfo systemInfo;
  private Session session ;
  @Override public void onCreate() {
    super.onCreate();
    ActiveAndroid.initialize(this);
    Stetho.initializeWithDefaults(this);
    instance = this;
    systemInfo = new SystemInfo(this);
    session = new Session();
  }
  public static MApp getApplication(){
    return instance;
  }
  public SystemInfo getSystemInfo(){
    return systemInfo;
  }
  public Session getSession(){
    return session;
  }
}
