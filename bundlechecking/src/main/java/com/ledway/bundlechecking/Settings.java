package com.ledway.bundlechecking;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by togb on 2016/4/23.
 */
public class Settings {
    private Context context;
    private String server;




    public Settings(Context context){
        this.context = context;
        reload();
    }





    public String getServer(){
        return server;
    }

    public void reload(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        server = sp.getString("Server", "www.ledway.com.tw:1433");
        sp.edit().putString("Server", server)
                .commit();
    }

}
