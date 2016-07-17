package com.ledway.btprinter;

import android.app.Application;
import com.activeandroid.ActiveAndroid;
import com.facebook.stetho.Stetho;
import com.ledway.btprinter.models.SystemInfo;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

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


    Picasso.Builder builder = new Picasso.Builder(this);
    builder.downloader(new OkHttpDownloader(this,Integer.MAX_VALUE));
    Picasso built = builder.build();
    built.setIndicatorsEnabled(true);
    built.setLoggingEnabled(true);
    Picasso.setSingletonInstance(built);
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
