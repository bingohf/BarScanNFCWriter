package com.ledway.btprinter.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.ledway.btprinter.R;
import java.io.IOException;

/**
 * Created by togb on 2017/12/17.
 */

public class ContextUtils {
 static Context mContext;
  public static void init(Context context){
    mContext = context;
  }

  public static String getMessage(Throwable throwable){
     if (throwable instanceof IOException){
       if (!isOnline()){
         return mContext.getString(R.string.network_is_not_available);
       }
     }
     return throwable.getMessage();
  }


  public static boolean isOnline() {
    ConnectivityManager cm =
        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    return netInfo != null && netInfo.isConnectedOrConnecting();
  }
}
