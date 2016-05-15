package com.ledway.barcodescannfcwriter;

import android.app.Application;
import com.activeandroid.ActiveAndroid;
import com.facebook.stetho.Stetho;
import com.ledway.framework.RemoteDB;

/**
 * Created by togb on 2016/3/27.
 */
public class MApp extends Application {
  private static MApp instance;
  private Settings settings;
  private UploadService uploadService;
  private RemoteDB licenseDB;

  public static MApp getInstance() {
    return instance;
  }

  public RemoteDB getLicenseDB() {
    return licenseDB;
  }

  @Override public void onCreate() {
    super.onCreate();
    ActiveAndroid.initialize(this);
    instance = this;
    settings = new Settings(this);
    uploadService = new UploadService(this);
    String connectionString =
        "jdbc:jtds:sqlserver://www.ledway.com.tw:1433;DatabaseName=WINUPRFID;charset=UTF8";
    licenseDB = new RemoteDB(connectionString);
    Stetho.initializeWithDefaults(this);
  }

  public Settings getSettings() {
    return settings;
  }

  public UploadService getUploadService() {
    return uploadService;
  }
}
