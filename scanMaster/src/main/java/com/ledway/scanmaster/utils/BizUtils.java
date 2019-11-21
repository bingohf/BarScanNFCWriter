package com.ledway.scanmaster.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ledway.scanmaster.AppConstants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by togb on 2018/2/14.
 */

public class BizUtils {
  public static String getMyTaxNo(Context context){

    final String DATA_FORMAT ="yyyyMMdd'T'HHmmss.S";
    final String ID_FORMAT ="%s-%s-LEDWAY-%s";
    SharedPreferences sp =
        context.getSharedPreferences(AppConstants.SP_NAME, Context.MODE_PRIVATE);
    String taxNo = sp.getString("MyTaxNo", "");
    if(!TextUtils.isEmpty(taxNo)) {
      return taxNo;
    }else {
      String mDeviceId =
          Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
      String mDeviceName = Build.MODEL;
      SimpleDateFormat mSimpleDateFormater = new SimpleDateFormat(DATA_FORMAT);
      String temp = String.format(ID_FORMAT,  mDeviceId,mDeviceName, mSimpleDateFormater.format(new Date()));
      return temp+ "~" + getLanguage();
    }
  }
  static String getLanguage() {
    Locale locale = Locale.getDefault();
    return locale.getLanguage() + "_" + locale.getCountry();
  }

  public static Observable<Bitmap> getQrCode(final String qrCode, final int size) {
    return Observable.create(new Observable.OnSubscribe<Bitmap>() {
      @Override public void call(Subscriber<? super Bitmap> subscriber) {
        QRCodeWriter writer = new QRCodeWriter();

        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        BitMatrix bitMatrix = null;
        try {
          bitMatrix = writer.encode(qrCode, BarcodeFormat.QR_CODE, size, size, hints);
          int width = bitMatrix.getWidth();
          int height = bitMatrix.getHeight();
          Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
          for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
              bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
          }
          subscriber.onNext(bmp);
          subscriber.onCompleted();
        } catch (WriterException e) {
          e.printStackTrace();
          subscriber.onError(e);
        }
      }
    });
  }
}
