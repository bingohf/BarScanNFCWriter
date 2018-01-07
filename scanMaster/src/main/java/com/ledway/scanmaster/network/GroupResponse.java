package com.ledway.scanmaster.network;

/**
 * Created by togb on 2018/1/7.
 */

public class GroupResponse {
  public Result[] result;
  public static class Result{
    public String myTaxNo;
    public String line;
  }
}
