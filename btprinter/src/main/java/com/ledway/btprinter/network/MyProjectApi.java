package com.ledway.btprinter.network;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.ledway.rxbus.RxBus;
import com.ledway.scanmaster.MApp;
import com.ledway.scanmaster.event.ServerChangedEvent;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.functions.Action1;

/**
 * Created by togb on 2016/8/7.
 */
public class MyProjectApi {
  private static MyProjectApi instance;
  private DBService dbService;
  private LedwayService ledwayService;

  private MyProjectApi(){
    buildSeApi();

    RxBus.getInstance().toObservable(ServerChangedEvent.class).subscribe(serverChangedEvent -> {
      buildSeApi();
    });
  }

  private void buildSeApi(){
    SharedPreferences sp =
        MApp.getInstance().getSharedPreferences("se_server", Context.MODE_PRIVATE);
    String se_server  = sp.getString("se_server", "");
    int se_port = sp.getInt("se_port", -1);
    String se_company = sp.getString("se_company", "");
    if(TextUtils.isEmpty(se_server) ){
      se_server = "http://ledwayvip.cloudapp.net";
    }
    if(TextUtils.isEmpty(se_company) ){
      se_company = "ledway";
    }
    if(se_port < 0 ){
      se_port = 8080;
    }
    String url = se_server + ":" + se_port +"/datasnap/rest/TLwDataModule/";


    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS);
    builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
    OkHttpClient client = builder.build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://www.ledway.com.tw/uploads/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .client(client)
        .build();
    ledwayService = retrofit.create(LedwayService.class);
    Retrofit retrofit2 = new Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .client(client)
        .build();
    dbService = retrofit2.create(DBService.class);
  }


  public static MyProjectApi getInstance(){
    if(instance == null){
      instance = new MyProjectApi();
    }
    return instance;
  }

  public LedwayService getLedwayService(){
    return ledwayService;
  }

  public DBService getDbService(){
    return dbService;
  }
}
