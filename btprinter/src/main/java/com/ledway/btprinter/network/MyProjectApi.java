package com.ledway.btprinter.network;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by togb on 2016/8/7.
 */
public class MyProjectApi {
  private static MyProjectApi instance;
  private LedwayService ledwayService;

  private MyProjectApi(){
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://www.ledway.com.tw/uploads/")
        .addConverterFactory(JacksonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .build();
    ledwayService = retrofit.create(LedwayService.class);
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
}
