package com.ledway.btprinter.adapters;

/**
 * Created by togb on 2016/6/4.
 */
public class TextData extends BaseData{
  public String text;
  public TextData(int type) {
    super(type);
  }
  public String getText(){
    return text;
  }
  public void setText(String value){
    text = value;
  }
}
