package com.ledway.btprinter.utils

import com.google.gson.Gson

class JsonUtils {
  companion object {
    private val sGson = Gson()
    fun<T> fromJson(json:String, cls:Class<T>) : T{
      return sGson.fromJson(json, cls)
    }
    fun toJson(any: Any):String{
      return sGson.toJson(any)
    }

  }
}