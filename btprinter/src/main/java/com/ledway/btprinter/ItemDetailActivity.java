package com.ledway.btprinter;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.serialport.api.SerialPort;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.Prod;
import com.ledway.framework.FullScannerActivity;
import com.ledway.framework.RemoteDB;
import com.zkc.Service.CaptureService;
import java.io.ByteArrayOutputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/5/29.
 */
public class ItemDetailActivity extends AppCompatActivity {
  private final static int RESULT_TAKE_PHOTO_1 = 1;
  private final static int RESULT_TAKE_PHOTO_2 = 2;
  private final static int RESULT_CAMERA_QR_CODE = 3;
  private SampleMaster mSampleMaster;
  private RecyclerView mListViewProd;
  private List<Map<String, String>> mProdList = new ArrayList<>();
  private DataAdapter mDataAdapter;
  private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver(){

    @Override
    public void onReceive(Context context, Intent intent) {
      String text = intent.getExtras().getString("code");
      if (text.length() < 10){
        Toast.makeText(ItemDetailActivity.this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
      }
      Pattern pattern = Pattern.compile("[^0-9a-zA-Z_ ]");
      if(!pattern.matcher(text).matches()) {
        appendBarCode(text);
      }
    }

  };


  private void appendBarCode(String text) {
    mSampleMaster.prods.add(new Prod(text));
    TextData textData = new TextData(DataAdapter.DATA_TYPE_BARCODE);
    textData.setText(text);
    mDataAdapter.addData(textData);

  }


  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSampleMaster = (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
    mSampleMaster.queryDetail();
    setContentView(R.layout.activity_item_detail);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    mListViewProd = (RecyclerView)findViewById(R.id.list_data);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("com.zkc.scancode");
    registerReceiver(scanBroadcastReceiver, intentFilter);
    setListView();
    setEvent();

    setView();
  }

  private void setView() {
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

  }

  private void setListView() {
    mDataAdapter = new DataAdapter(this);
    mDataAdapter.init();
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    mListViewProd.setLayoutManager(linearLayoutManager);
    mListViewProd.setAdapter(mDataAdapter);
    mListViewProd.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
      @Override public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
      }

      @Override public void onTouchEvent(RecyclerView rv, MotionEvent e) {

      }

      @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

      }
    });

  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mSampleMaster.allSave();
    unregisterReceiver(scanBroadcastReceiver);
  }

  private void setEvent() {
    findViewById(R.id.btn_scan_barcode).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        SerialPort.CleanBuffer();
        CaptureService.scanGpio.openScan();
      }
    });

    findViewById(R.id.btn_take_photo_1).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePicture, RESULT_TAKE_PHOTO_1);//zero can be replaced with any action code
      }
    });
    findViewById(R.id.btn_take_photo_2).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePicture, RESULT_TAKE_PHOTO_2);//zero can be replaced with any action code
      }
    });
    findViewById(R.id.btn_qr_code).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startActivityForResult(new Intent(ItemDetailActivity.this, FullScannerActivity.class), RESULT_CAMERA_QR_CODE);
      }
    });
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.action_upload:{
        uploadRecord();
        break;
      }
      case R.id.action_print:{
        startActivity(new Intent(this, PrintPreviewActivity.class));
        break;
      }
    }
    return true;
  }

  private void uploadRecord(){
    final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.save_record), getString(R.string.wait_a_moment));
    mSampleMaster.remoteSave().subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<SampleMaster>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
          }

          @Override public void onError(Throwable e) {
            progressDialog.dismiss();
            Toast.makeText(ItemDetailActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
          }

          @Override public void onNext(SampleMaster sampleMaster) {

          }
        });
  }
  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.item_detail_menu,menu);
    return true;

  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      switch (requestCode) {
        case RESULT_TAKE_PHOTO_1:
        case RESULT_TAKE_PHOTO_2: {
          if (resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            PhotoData photoData = new PhotoData(RESULT_TAKE_PHOTO_2 == requestCode? DataAdapter.DATA_TYPE_PHOTO_2: DataAdapter.DATA_TYPE_PHOTO_1);
            photoData.setBitmap(photo);
            mDataAdapter.addData(photoData);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            if (RESULT_TAKE_PHOTO_1 == requestCode) {
              mSampleMaster.image1 = byteArray;
            } else {
              mSampleMaster.image2 = byteArray;
            }
          }
          break;
        }
        case RESULT_CAMERA_QR_CODE: {

          String qrcode = data.getStringExtra("barcode");
          TextData textData = new TextData(DataAdapter.DATA_TYPE_MEMO);
          textData.setText(qrcode);
          mSampleMaster.desc = qrcode;
          mDataAdapter.addData(textData);
          break;
        }
      }
    }
  }
}

