package com.ledway.btprinter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.domain.BTPrinter;
import com.ledway.btprinter.models.Prod;
import com.ledway.btprinter.models.SampleMaster;
import rx.Subscriber;
import rx.schedulers.Schedulers;

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

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.bt_print_preview_menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.action_print:{
        doPrint();
        break;
      }
    }
    return  true;
  }

  private void doPrint() {
    BTPrinter btPrinter = BTPrinter.getBtPrinter();
    PhotoData photoData = (PhotoData) mDataAdapter.getItem(mDataAdapter.getItemCount() -1);
    btPrinter.printBitmap(photoData.getBitmap()).subscribeOn(Schedulers.io())
    .subscribe(new Subscriber<Boolean>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        Log.e("bt_print", e.getMessage(), e);
        Toast.makeText(PrintPreviewActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
      }

      @Override public void onNext(Boolean aBoolean) {

      }
    });
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
      BitMatrix bitMatrix = writer.encode(mSampleMaster.qrcode, BarcodeFormat.QR_CODE, 200, 200);
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
