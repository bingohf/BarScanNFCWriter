package com.ledway.barcodescannfcwriter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by togb on 2016/4/23.
 */
public class Settings {
    private Context context;
    private String line;
    private String reader;
    private String server;
    private boolean isAutoUpload;
    private String deviceType;

    public Settings(Context context){
        this.context = context;
        reload();
    }


    public String getReader() {
        return reader;
    }

    public String getLine() {
        return line;
    }



    public String getServer(){
        return server;
    }

    public void reload(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        line = sp.getString("Line", "");
        reader = sp.getString("Reader", "");
        server = sp.getString("Server", "www.ledway.com.tw:1433");
        isAutoUpload = sp.getBoolean("auto_upload_switch", true);
        deviceType = sp.getString("deviceType", "ReadNFC");

        sp.edit().putString("Line", line)
                .putString("Reader", reader)
                .putString("Server", server)
                .putBoolean("auto_upload_switch", isAutoUpload)
                .putString("deviceType", deviceType)
                .commit();
    }

}
