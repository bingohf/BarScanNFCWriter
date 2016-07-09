package com.ledway.btprinter;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ledway.btprinter.adapters.BaseData;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.domain.BTPrinter;
import com.ledway.btprinter.fragments.BindBTPrintDialogFragment;
import com.ledway.btprinter.models.Prod;
import com.ledway.btprinter.models.SampleMaster;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.w3c.dom.Text;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
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

    final BTPrinter btPrinter = BTPrinter.getBtPrinter();
    if (TextUtils.isEmpty(btPrinter.getMacAddress())){
      BindBTPrintDialogFragment bindBTPrintDialogFragment = new BindBTPrintDialogFragment();
      bindBTPrintDialogFragment.show(getSupportFragmentManager(), "dialog");
      return;
    }

    final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.action_print), getString(R.string.wait_a_moment));
    Observable.from(mDataAdapter)
        .filter(new Func1<BaseData, Boolean>() {
          @Override public Boolean call(BaseData baseData) {
            return baseData.getType() != DataAdapter.DATA_TYPE_PHOTO_1 &&baseData.getType() != DataAdapter.DATA_TYPE_PHOTO_2 ;
          }
        })
        .flatMap(new Func1<BaseData, Observable<Boolean>>() {
          @Override public Observable<Boolean> call(BaseData baseData) {
            return btPrinter.print(baseData);
          }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Boolean>() {
          @Override public void onCompleted() {
            btPrinter.println("\n").subscribe();
            progressDialog.dismiss();
          }

          @Override public void onError(Throwable e) {
            Log.e("error", e.getMessage(),e);
            progressDialog.dismiss();
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

    PhotoData logoData = new PhotoData(DataAdapter.DATA_TYPE_LOGO);
    Bitmap bmpLogo = BitmapFactory.decodeResource(getResources(),
        R.drawable.logo);
    logoData.setBitmap(bmpLogo);
    mDataAdapter.addData(logoData);
    if (!TextUtils.isEmpty(mSampleMaster.getDesc())){
      TextData textData = new TextData(DataAdapter.DATA_TYPE_MEMO);
      textData.setText(mSampleMaster.getDesc());
      mDataAdapter.addData(textData);
    }

/*    if (mSampleMaster.getImage1() != null){
      Bitmap bitmap =  BitmapFactory.decodeByteArray(mSampleMaster.getImage1() , 0, mSampleMaster.getImage1() .length);
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_PHOTO_1);
      photoData.setBitmap(bitmap);
      mDataAdapter.addData(photoData);
    }

    if (mSampleMaster.getImage2() != null){
      Bitmap bitmap =  BitmapFactory.decodeByteArray(mSampleMaster.getImage2() , 0, mSampleMaster.getImage2() .length);
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_PHOTO_2);
      photoData.setBitmap(bitmap);
      mDataAdapter.addData(photoData);
    }*/

    Iterator<Prod> iterator = mSampleMaster.prodIterator();
    while(iterator.hasNext()){
      Prod prod = iterator.next();
      TextData textData = new TextData(DataAdapter.DATA_TYPE_BARCODE);
      textData.setText(prod.ext +": " + prod.barcode);
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
