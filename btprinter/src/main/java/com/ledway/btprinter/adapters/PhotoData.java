package com.ledway.btprinter.adapters;

import android.graphics.Bitmap;

/**
 * Created by togb on 2016/6/4.
 */
public class PhotoData extends BaseData {
  private Bitmap bitmap;
  public Bitmap getBitmap() {
    return bitmap;
  }

  public void setBitmap(Bitmap bitmap) {
    this.bitmap = bitmap;
  }


  public PhotoData(int type) {
    super(type);
  }


}
