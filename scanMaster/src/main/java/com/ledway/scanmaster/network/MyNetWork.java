package com.ledway.scanmaster.network;

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
 * Created by togb on 2018/1/7.
 */

public class MyNetWork {
  private static ServiceApi serviceApi;
  public static ServiceApi getServiceApi(){
    if(serviceApi == null){
      build();
      RxBus.getInstance().toObservable(ServerChangedEvent.class)
          .subscribe(serverChangedEvent -> {
        build();
      });

    }
    return serviceApi;
  }

  private static void build(){
    SharedPreferences sp =
        MApp.getInstance().getSharedPreferences("sm_server", Context.MODE_PRIVATE);
    String sm_server  = sp.getString("sm_server", "");
    int sm_port = sp.getInt("sm_port", -1);
    String sm_company = sp.getString("sm_company", "");
    if(TextUtils.isEmpty(sm_server) ){
      sm_server = "http://ledwayvip.cloudapp.net";
    }
    if(TextUtils.isEmpty(sm_company) ){
      sm_company = "ledway";
    }
    if(sm_port < 0 ){
      sm_port = 8080;
    }
    try{
      OkHttpClient.Builder builder = new OkHttpClient.Builder();
      builder.writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS);
      builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
      OkHttpClient client = builder.build();
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl( sm_server + ":" + sm_port + "/datasnap/rest/TLwDataModule/")
          .addConverterFactory(GsonConverterFactory.create())
          .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
          .client(client)
          .build();
      serviceApi = retrofit.create(ServiceApi.class);
    }catch (Exception e){
      MApp.getInstance().getSharedPreferences("sm_server", Context.MODE_PRIVATE).edit()
          .putInt("sm_port", -1).putString("sm_server","").putString("sm_company", "").commit();
      build();
    }



  }


}
