package com.ledway.btprinter.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import java.lang.reflect.Modifier
import java.util.*

class JsonUtils {
  companion object {
    private val sGson = GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT,
        Modifier.STATIC)
        .registerTypeAdapter(Date::class.java,
            JsonSerializer<Date> { src, _, _ -> JsonPrimitive(src?.time) })
        .registerTypeAdapter(Date::class.java,
            JsonDeserializer<Date> { json, _, _ -> Date(json!!.asJsonPrimitive.asLong) })

        .create()


    fun <T> fromJson(json: String, cls: Class<T>): T {
      return sGson.fromJson(json, cls)
    }

    fun toJson(any: Any): String {
      return sGson.toJson(any)
    }

  }
}