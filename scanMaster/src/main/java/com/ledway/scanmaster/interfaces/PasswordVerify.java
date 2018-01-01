package com.ledway.scanmaster.interfaces;

/**
 * Created by togb on 2017/3/5.
 */

public interface PasswordVerify {
  boolean verify();
  void userPassword(String password);
}
