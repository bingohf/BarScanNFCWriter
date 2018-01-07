package com.ledway.scanmaster.network;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by togb on 2018/1/7.
 */

public class MyNetWork {
  private static ServiceApi serviceApi;
  public static ServiceApi getServiceApi(){
    if(serviceApi == null){
      build();
    }
    return serviceApi;

  }

  private static void build(){
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS);
    builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
    OkHttpClient client = builder.build();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://ledwayvip.cloudapp.net:8080/datasnap/rest/TLwDataModule/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .client(client)
        .build();
    serviceApi = retrofit.create(ServiceApi.class);
  }
}
