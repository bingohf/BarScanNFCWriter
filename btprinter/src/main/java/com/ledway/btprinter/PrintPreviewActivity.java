package com.ledway.btprinter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;

/**
 * Created by togb on 2016/6/12.
 */
public class PrintPreviewActivity extends AppCompatActivity {
  private RecyclerView mListView;
  private DataAdapter mDataAdapter;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_print_preview);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    mListView = (RecyclerView) findViewById(R.id.list_data);
    setListView();
  }

  private void setListView() {
    mDataAdapter = new DataAdapter(this);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    mListView.setLayoutManager(linearLayoutManager);
    mListView.setAdapter(mDataAdapter);
    QRCodeWriter writer = new QRCodeWriter();
    try {
      BitMatrix bitMatrix = writer.encode("http://www.ledway.com.tw", BarcodeFormat.QR_CODE, 200, 200);
      int width = bitMatrix.getWidth();
      int height = bitMatrix.getHeight();
      Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
        }
      }
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_QR_CODE);
      photoData.setBitmap(bmp);
      mDataAdapter.addData(photoData);
    } catch (WriterException e) {
      e.printStackTrace();
    }
  }
}
