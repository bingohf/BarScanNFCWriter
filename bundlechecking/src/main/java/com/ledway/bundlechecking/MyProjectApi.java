package com.ledway.bundlechecking;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.serialport.api.MyApp;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Created by togb on 2016/5/1.
 */
public class MyProjectApi {
  private static MyProjectApi instance;
  private Context context;
  private Settings settings;
  private CallableStatement cstmt;

  private MyProjectApi(Context context){
    this.context = context;
    settings = MApp.getInstance().getSettings();
  }

  public static MyProjectApi getInstance(){
    if (instance == null){
      instance = new MyProjectApi(MApp.getInstance());
    }
    return instance;
  }

  public Observable<String> getBarCodeDesc(final String barcode){
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {
        if (!isOnline()){
          subscriber.onError(new Exception("Network is not available"));
          if(cstmt != null) {
            try {
              cstmt.close();
            } catch (SQLException e) {
              e.printStackTrace();
            }
            cstmt = null;
          }
        }else {
          try {
            prepareStatement();
            cstmt.setString(1, barcode);
            cstmt.registerOutParameter(2, Types.VARCHAR);
            cstmt.execute();
            subscriber.onNext(cstmt.getString(2));
            subscriber.onCompleted();
          } catch (Exception e) {
            e.printStackTrace();
            subscriber.onError(e);
          }
        }
      }
    }).doOnError(new Action1<Throwable>() {
      @Override
      public void call(Throwable throwable) {
        cstmt = null;
      }
    }).retry(2);
  }

  private synchronized void prepareStatement() throws Exception {
    boolean isClosed = true;
    try {
      if(cstmt != null) {
        isClosed = cstmt.isClosed();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    if (cstmt == null || isClosed){
      Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
      String connectionString = "jdbc:jtds:sqlserver://%s;DatabaseName=WINUPRFID;charset=UTF8";
      connectionString = String.format(connectionString, settings.getServer());
      Connection conn = DriverManager.getConnection(connectionString,
          "sa", "ledway");
      cstmt = conn.prepareCall("{call sp_queryByBarCode(?,?)}");

    }
  }
  public void reset(){
    cstmt = null;
  }
  public boolean isOnline() {
    ConnectivityManager cm =
        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = cm.getActiveNetworkInfo();
    return netInfo != null && netInfo.isConnectedOrConnecting();
  }
}
