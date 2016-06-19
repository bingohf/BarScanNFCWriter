package com.ledway.btprinter.adapters;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by togb on 2016/6/4.
 */
public class BaseData {
  protected int type;
  public BaseData(int type){
    this.type = type;
  }

  public int getType(){
    return type;
  }



  public void printTo(OutputStream outputStream) throws IOException {

  }
}
