package com.ledway.btprinter.domain;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.adapters.BaseData;
import com.ledway.btprinter.fragments.BindBTPrintDialogFragment;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Created by togb on 2016/6/19.
 */
public class BTPrinter {
  private static BTPrinter instance = null;

  public String getMacAddress() {
    return macAddress;
  }

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
      if (mOutput != null) {
        mOutput.close();
      }
      if(mSocket != null) {
        mSocket.close();
      }
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

  public Observable<Boolean> print(final BaseData baseData){
    return Observable.create(new Observable.OnSubscribe<Boolean>() {
      @Override public void call(Subscriber<? super Boolean> subscriber) {
        try {
          prepareOutput();
          baseData.printTo(mOutput);
          subscriber.onNext(true);
          Thread.sleep(700);
          subscriber.onCompleted();
        } catch (IOException e) {
          e.printStackTrace();
          subscriber.onError(e);
          close();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).retry(2);
  };

  private void prepareOutput() throws IOException {
    BluetoothDevice device =
        BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
    if(mSocket == null) {
      mSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_SECURE);
      mSocket.connect();
    }
    mOutput =  mSocket.getOutputStream();
  }



}
