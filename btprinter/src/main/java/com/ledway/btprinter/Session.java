package com.ledway.btprinter;

import java.util.HashMap;
import java.util.Objects;

/**
 * Created by togb on 2016/6/5.
 */
public class Session {
  private HashMap<String,Object> hashMap = new HashMap<>();
  public void put(String key, Object value){
    hashMap.put(key, value);
  }
  public Object getValue(String key){
    return hashMap.get(key);
  }
}
