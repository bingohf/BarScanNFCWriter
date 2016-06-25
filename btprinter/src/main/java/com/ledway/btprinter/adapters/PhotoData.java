package com.ledway.btprinter.adapters;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import java.io.IOException;
import java.io.OutputStream;

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
  public static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
    Bitmap BitmapOrg = bitmap;
    int width = BitmapOrg.getWidth();
    int height = BitmapOrg.getHeight();
    int newWidth = w;
    int newHeight = h;

    float scaleWidth = ((float) newWidth) / width;
    float scaleHeight = ((float) newHeight) / height;
    Matrix matrix = new Matrix();
    matrix.postScale(scaleWidth, scaleWidth);
    Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
        height, matrix, true);
    return resizedBitmap;
  }

  @Override public void printTo(OutputStream outputStream) throws IOException {
    String label = "";
    switch (type){
      case DataAdapter.DATA_TYPE_PHOTO_1:{
        label = MApp.getApplication().getString(R.string.card1);
        break;
      }
      case DataAdapter.DATA_TYPE_PHOTO_2:{
        label = MApp.getApplication().getString(R.string.card2);
        break;
      }
    }
    if (!label.isEmpty()){
      outputStream.write(new byte[]{0x1b,0x40});
      outputStream.write(label.getBytes("GBK"));
      outputStream.write(new byte[]{0x0a});
      outputStream.flush();
    }


    Bitmap bitmap = this.getBitmap();
    if (bitmap != null) {
      if (bitmap.getHeight() > 384) {
        bitmap = resizeImage(bitmap, 384, 384);
      } else {
        bitmap = resizeImage(bitmap, 384, bitmap.getHeight());
      }
      byte[] sendbuf = StartBmpToPrintCode(bitmap);
      outputStream.write(sendbuf);
      outputStream.flush();
    }
  }

  private byte[] StartBmpToPrintCode(Bitmap bitmap) {
    byte temp = 0;
    int j = 7;
    int start = 0;
    if(bitmap == null) {
      return null;
    } else {
      int mWidth = bitmap.getWidth();
      int mHeight = bitmap.getHeight();
      int[] mIntArray = new int[mWidth * mHeight];
      byte[] data = new byte[mWidth * mHeight];
      bitmap.getPixels(mIntArray, 0, mWidth, 0, 0, mWidth, mHeight);
      this.encodeYUV420SP(data, mIntArray, mWidth, mHeight);
      byte[] result = new byte[mWidth * mHeight / 8];

      int aHeight;
      for(aHeight = 0; aHeight < mWidth * mHeight; ++aHeight) {
        temp += (byte)(data[aHeight] << j);
        --j;
        if(j < 0) {
          j = 7;
        }

        if(aHeight % 8 == 7) {
          result[start++] = temp;
          temp = 0;
        }
      }

      if(j != 7) {
        result[start++] = temp;
      }

      aHeight = 24 - mHeight % 24;
      int perline = mWidth / 8;
      byte[] add = new byte[aHeight * perline];
      byte[] nresult = new byte[mWidth * mHeight / 8 + aHeight * perline];
      System.arraycopy(result, 0, nresult, 0, result.length);
      System.arraycopy(add, 0, nresult, result.length, add.length);
      byte[] byteContent = new byte[(mWidth / 8 + 4) * (mHeight + aHeight)];
      byte[] bytehead = new byte[]{(byte)31, (byte)16, (byte)(mWidth / 8), (byte)0};

      for(int index = 0; index < mHeight + aHeight; ++index) {
        System.arraycopy(bytehead, 0, byteContent, index * (perline + 4), 4);
        System.arraycopy(nresult, index * perline, byteContent, index * (perline + 4) + 4, perline);
      }

      return byteContent;
    }
  }
  private void encodeYUV420SP(byte[] yuv420sp, int[] rgba, int width, int height) {
    int frameSize = width * height;
    int[] U = new int[frameSize];
    int[] V = new int[frameSize];
    int uvwidth = width / 2;
    boolean bits = true;
    int index = 0;
    boolean f = false;

    for(int j = 0; j < height; ++j) {
      for(int i = 0; i < width; ++i) {
        int r = (rgba[index] & -16777216) >> 24;
        int g = (rgba[index] & 16711680) >> 16;
        int b = (rgba[index] & '\uff00') >> 8;
        int y = (66 * r + 129 * g + 25 * b + 128 >> 8) + 16;
        int u = (-38 * r - 74 * g + 112 * b + 128 >> 8) + 128;
        int v = (112 * r - 94 * g - 18 * b + 128 >> 8) + 128;
        byte temp = (byte)(y > 255?255:(y < 0?0:y));
        yuv420sp[index++] = (byte)(temp > 0?1:0);
      }
    }

    f = false;
  }
}
