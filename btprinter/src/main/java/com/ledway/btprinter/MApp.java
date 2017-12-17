package com.ledway.btprinter;

import android.app.Activity;
import android.app.Application;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import com.activeandroid.ActiveAndroid;
import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.ledway.btprinter.models.SystemInfo;
import com.ledway.btprinter.utils.ContextUtils;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import io.fabric.sdk.android.Fabric;
import java.io.File;

/**
 * Created by togb on 2016/5/29.
 */
public class MApp extends Application {
  private static MApp instance;
  private SystemInfo systemInfo;
  private Session session ;
  public Point point = new Point();
  @Override public void onCreate() {
    super.onCreate();
    Fabric.with(this, new Crashlytics());
    ContextUtils.init(this);
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

    registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
      @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        display.getSize(point);
      }

      @Override public void onActivityStarted(Activity activity) {

      }

      @Override public void onActivityResumed(Activity activity) {

      }

      @Override public void onActivityPaused(Activity activity) {

      }

      @Override public void onActivityStopped(Activity activity) {

      }

      @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

      }

      @Override public void onActivityDestroyed(Activity activity) {

      }
    });
  }
  public String getPicPath(){

    File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    return storageDir.getAbsolutePath();
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
