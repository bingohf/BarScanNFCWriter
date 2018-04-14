package com.ledway.scanmaster;

import android.app.Application;
import android.content.Intent;
import com.ledway.scanmaster.utils.LogDebugTree;
import timber.log.Timber;

/**
 * Created by togb on 2017/2/18.
 */

public class MApp extends Application {
  private AppComponent appComponent;
  private static MApp sInstance;
  @Override public void onCreate() {
    super.onCreate();
    Timber.plant(new LogDebugTree(this));
    appComponent = DaggerAppComponent.builder().appModule(new AppModule(this)).build();
    sInstance = this;
    startScanService();
    crashLog();
  }

  private void crashLog() {
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      Timber.i("Crashed : %s - %d", thread.getName(), thread.getId());
      Timber.e(throwable, throwable.getMessage());
    });
  }

  private void startScanService() {
/*    Intent newIntent = new Intent(this, CaptureService.class);
    startService(newIntent);*/
  }

  public AppComponent getAppComponet(){
    return appComponent;
  }
  public static MApp getInstance(){
    return sInstance;
  }

}
