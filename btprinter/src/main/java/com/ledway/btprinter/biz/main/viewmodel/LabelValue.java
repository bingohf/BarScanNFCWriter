package com.ledway.btprinter.biz.main.viewmodel;

public class LabelValue<T> {
  public LabelValue(T value, String label) {
    this.value = value;
    this.label = label;
  }

  public T value;
  public String label;
}
