package com.ledway.btprinter.biz.main.viewmodel;

public class DataPackage<T1,T2,T3> {
  public DataPackage(T1 value, T2 value2, T3 value3) {
    this.value = value;
    this.value2 = value2;
    this.value3 = value3;
  }
  public T1 value;
  public T2 value2;
  private T3 value3;

}
