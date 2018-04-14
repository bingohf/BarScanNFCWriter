package com.ledway.btprinter.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import java.util.Hashtable;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/8/28.
 */
public class ShareAppFragment extends PagerFragment {
  private ImageView mImageView;

  @Override public String getTitle() {
    return MApp.getApplication().getString(R.string.titile_share_app);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_my_id, container, false);
    mImageView = (ImageView) view.findViewById(R.id.img_qrcode);
    return view;
  }

  @Override public void onStart() {
    super.onStart();
    mImageView.setImageDrawable(ContextCompat.getDrawable(mImageView.getContext(), R.drawable.app_share_qrcode));
/*    getQrCode("http://www.ledway.com.tw/uploads/sales_edge.apk", 300)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
        new Action1<Bitmap>() {
          @Override public void call(Bitmap bitmap) {
            mImageView.setImageBitmap(bitmap);
          }
        });*/
  }

  private Observable<Bitmap> getQrCode(final String qrCode, final int size) {
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
