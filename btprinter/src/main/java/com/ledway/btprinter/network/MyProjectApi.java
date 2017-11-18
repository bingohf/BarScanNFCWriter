package com.ledway.btprinter.network;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by togb on 2016/8/7.
 */
public class MyProjectApi {
  private static MyProjectApi instance;
  private final DBService dbService;
  private LedwayService ledwayService;

  private MyProjectApi(){
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS);
    builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
    OkHttpClient client = builder.build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://www.ledway.com.tw/uploads/")
        .addConverterFactory(JacksonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .client(client)
        .build();
    ledwayService = retrofit.create(LedwayService.class);



    Retrofit retrofit2 = new Retrofit.Builder()
        .baseUrl("http://ledwayvip.cloudapp.net:8080/datasnap/rest/TLwDataModule/")
        .addConverterFactory(JacksonConverterFactory.create())
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
