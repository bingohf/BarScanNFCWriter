package com.ledway.barcodescannfcwriter;

import android.app.Application;

import com.activeandroid.ActiveAndroid;

/**
 * Created by togb on 2016/3/27.
 */
public class MApp extends Application {
    private static MApp instance;
    private Settings settings;
    private UploadService uploadService;

   @Override
    public void onCreate() {
        super.onCreate();
        ActiveAndroid.initialize(this);
       instance = this;
        settings = new Settings(this);
       uploadService = new UploadService(this);
    }
    public Settings getSettings() {
        return settings;
    }
    public static MApp getInstance(){
        return instance;
    }
    public UploadService getUploadService() {
        return uploadService;
    }
}
