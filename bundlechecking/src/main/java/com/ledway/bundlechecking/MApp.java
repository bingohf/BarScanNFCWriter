package com.ledway.bundlechecking;

import android.app.Application;

/**
 * Created by togb on 2016/5/1.
 */
public class MApp extends Application {
  private static MApp instance;
  private Settings settings;

  @Override public void onCreate() {
    super.onCreate();
    instance = this;
    settings = new Settings(this);
  }

  public static MApp getInstance(){
    return instance;
  }
  public Settings getSettings(){
    return settings;
  }

}
