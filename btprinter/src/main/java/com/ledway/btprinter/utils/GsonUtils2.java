package com.ledway.btprinter.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Created by togb on 2017/12/31.
 */

public class GsonUtils2 {
  private static final Gson sGson;
  static {
    GsonBuilder gsonBuilder = new GsonBuilder();
    sGson = gsonBuilder.create();
  }

  public static<T> T fromJson(String json, Class<T> cls){
    return sGson.fromJson(json,cls );
  }
}
