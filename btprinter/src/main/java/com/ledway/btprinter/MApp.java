package com.ledway.btprinter;

import android.app.Application;
import com.activeandroid.ActiveAndroid;

/**
 * Created by togb on 2016/5/29.
 */
public class MApp extends Application {
  @Override public void onCreate() {
    super.onCreate();
    ActiveAndroid.initialize(this);
  }
}
