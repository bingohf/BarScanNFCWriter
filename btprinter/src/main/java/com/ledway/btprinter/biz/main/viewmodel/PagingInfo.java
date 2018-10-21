package com.ledway.btprinter.biz.main.viewmodel;

public class PagingInfo<T> {
  public PagingInfo(int max, int position, T data) {
    this.max = max;
    this.position = position;
    this.data = data;
  }

  public int max;
  public int position;
  public T data;
}
