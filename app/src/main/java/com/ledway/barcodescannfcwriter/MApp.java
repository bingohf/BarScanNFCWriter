package com.ledway.barcodescannfcwriter;

import android.app.Application;

import com.activeandroid.ActiveAndroid;

/**
 * Created by togb on 2016/3/27.
 */
public class MApp extends Application {
    private Settings settings;
    private static MApp instance;
   @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
       instance = this;
        settings = new Settings(this);
    }
    public Settings getSettings() {
        return settings;
    }
    public static MApp getInstance(){
        return instance;
    }

}
