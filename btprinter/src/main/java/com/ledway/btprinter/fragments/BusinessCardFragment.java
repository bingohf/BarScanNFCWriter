package com.ledway.btprinter.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by togb on 2016/8/28.
 */
public class BusinessCardFragment extends PagerFragment {
  private SharedPreferences sp;
  private EditText editText;
  private ImageView imageView;

  @Override public String getTitle() {
    return MApp.getApplication().getString(R.string.title_my_business_card);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sp = getActivity().getSharedPreferences("qrcode", Context.MODE_PRIVATE);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_my_business_card, container, false);


    initView(view);
    return view;
  }

  private void initView(View view) {
    editText  = (EditText)view. findViewById(R.id.edt_text);
    imageView = (ImageView) view.findViewById(R.id.img_qrcode);
    RxTextView.textChanges(editText).debounce(1, TimeUnit.SECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<CharSequence>() {
          @Override public void call(CharSequence charSequence) {
            saveInput();
            try {
              if (!TextUtils.isEmpty(charSequence)) {
                str2QRCode(charSequence.toString());
              }
            } catch (WriterException e) {
              e.printStackTrace();
            }
          }
        });
    String text = sp.getString("qrcode",getString(R.string.default_my_business_card));
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
    imageView.setImageBitmap(bmp);
  }

  private void saveInput() {
    sp.edit().putString("qrcode",editText.getText().toString() ).apply();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    saveInput();
  }
}
