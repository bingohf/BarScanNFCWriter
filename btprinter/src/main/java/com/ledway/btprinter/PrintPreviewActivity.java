package com.ledway.btprinter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.models.Prod;
import com.ledway.btprinter.models.SampleMaster;

/**
 * Created by togb on 2016/6/12.
 */
public class PrintPreviewActivity extends AppCompatActivity {
  private RecyclerView mListView;
  private DataAdapter mDataAdapter;
  private SampleMaster mSampleMaster;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_print_preview);
    mSampleMaster  = (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    mListView = (RecyclerView) findViewById(R.id.list_data);
    setListView();
  }

  private void setListView() {
    mDataAdapter = new DataAdapter(this);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    mListView.setLayoutManager(linearLayoutManager);
    mListView.setAdapter(mDataAdapter);

    if (!TextUtils.isEmpty(mSampleMaster.desc)){
      TextData textData = new TextData(DataAdapter.DATA_TYPE_MEMO);
      textData.setText(mSampleMaster.desc);
      mDataAdapter.addData(textData);
    }

    if (mSampleMaster.image1 != null){
      Bitmap bitmap =  BitmapFactory.decodeByteArray(mSampleMaster.image1 , 0, mSampleMaster.image1 .length);
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_PHOTO_1);
      photoData.setBitmap(bitmap);
      mDataAdapter.addData(photoData);
    }

    if (mSampleMaster.image2 != null){
      Bitmap bitmap =  BitmapFactory.decodeByteArray(mSampleMaster.image2 , 0, mSampleMaster.image2 .length);
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_PHOTO_2);
      photoData.setBitmap(bitmap);
      mDataAdapter.addData(photoData);
    }

    for (Prod prod: mSampleMaster.prods){
      TextData textData = new TextData(DataAdapter.DATA_TYPE_BARCODE);
      textData.setText(prod.barcode);
      mDataAdapter.addData(textData);
    }



    try {
      QRCodeWriter writer = new QRCodeWriter();
      BitMatrix bitMatrix = writer.encode(mSampleMaster.qrcode, BarcodeFormat.QR_CODE, 100, 100);
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
