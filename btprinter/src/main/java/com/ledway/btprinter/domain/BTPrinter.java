package com.ledway.btprinter.domain;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import com.ledway.btprinter.MApp;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by togb on 2016/6/19.
 */
public class BTPrinter {
  private static BTPrinter instance = null;
  private String macAddress;
  private static final UUID MY_UUID_SECURE =
      UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private BluetoothSocket mSocket;
  private OutputStream mOutput;

  private BTPrinter(String macAddress){
    this.macAddress = macAddress;
  }
  public static BTPrinter getBtPrinter(){
    String macAddress = MApp.getApplication().
        getSharedPreferences("bt_printer", Context.MODE_PRIVATE).getString("mac_address", "");
    if (instance != null && !instance.macAddress.equals(macAddress)){
      instance.close();
      instance = null;
    }
    if (instance == null){
      instance = new BTPrinter(macAddress);
    }
    return instance;

  }

  private void close() {
    try {
      mOutput.close();
      mSocket.close();
      mSocket = null;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Observable<Boolean> println(final String text){
   return Observable.create(new Observable.OnSubscribe<Boolean>() {
     @Override public void call(Subscriber<? super Boolean> subscriber) {
       try {
          prepareOutput();
         mOutput.write(new byte[]{0x1b,0x40});
         mOutput.write(text.getBytes("GBK"));
         mOutput.write(new byte[]{0x0a});
       } catch (IOException e) {
         e.printStackTrace();
         subscriber.onError(e);
         close();
       }
     }
   }).retry(2);

  }
  public Observable<Boolean> printBitmap(final Bitmap bitmap){
    return Observable.create(new Observable.OnSubscribe<Boolean>() {
      @Override public void call(Subscriber<? super Boolean> subscriber) {
        try {
          prepareOutput();
          byte[] sendbuf = StartBmpToPrintCode(bitmap);
          mOutput.write(sendbuf);
        } catch (IOException e) {
          e.printStackTrace();
          subscriber.onError(e);
          close();
        }
      }
    }).retry(2);
  }

  private void prepareOutput() throws IOException {
    BluetoothDevice device =
        BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
    if(mSocket == null) {
      mSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_SECURE);
      mSocket.connect();
    }
    mOutput =  mSocket.getOutputStream();
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
