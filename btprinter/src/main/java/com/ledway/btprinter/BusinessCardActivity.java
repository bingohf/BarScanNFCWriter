package com.ledway.btprinter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by togb on 2016/7/30.
 */
public class BusinessCardActivity extends AppCompatActivity {
  private SharedPreferences sp;
  private EditText editText;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_bussiness_card);
    sp = getSharedPreferences("qrcode", Context.MODE_PRIVATE);
    initView();

  }

  @Override protected void onDestroy() {
    super.onDestroy();
    saveInput();
  }

  private void saveInput() {
   sp.edit().putString("qrcode",editText.getText().toString() ).apply();
  }

  private void initView() {
    editText  = (EditText) findViewById(R.id.edt_text);
    RxTextView.textChanges(editText).debounce(1, TimeUnit.SECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<CharSequence>() {
          @Override public void call(CharSequence charSequence) {
            try {
              if (!TextUtils.isEmpty(charSequence)) {
                str2QRCode(charSequence.toString());
              }
            } catch (WriterException e) {
              e.printStackTrace();
            }
          }
        });
    String text = sp.getString("qrcode","");
    if (TextUtils.isEmpty(text)){
      text = getString(R.string.default_my_business_card);
    }
    editText.setText(text);
  }

  private void str2QRCode(String qrCode) throws WriterException {
    QRCodeWriter writer = new QRCodeWriter();

    Hashtable<EncodeHintType,Object> hints = new Hashtable<EncodeHintType,Object>();
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
    BitMatrix bitMatrix = writer.encode(qrCode, BarcodeFormat.QR_CODE, 200, 200,hints);
    int width = bitMatrix.getWidth();
    int height = bitMatrix.getHeight();
    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
      }
    }
    ImageView imageView = (ImageView) findViewById(R.id.img_qrcode);
    imageView.setImageBitmap(bmp);
  }
}
