package com.ledway.btprinter;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ledway.btprinter.adapters.BaseData;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.domain.BTPrinter;
import com.ledway.btprinter.fragments.BindBTPrintDialogFragment;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
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
    mSampleMaster = (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
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
    switch (item.getItemId()) {
      case R.id.action_print: {
        doPrint();
        break;
      }
    }
    return true;
  }

  private void doPrint() {

    final BTPrinter btPrinter = BTPrinter.getBtPrinter();
    if (TextUtils.isEmpty(btPrinter.getMacAddress())) {
      BindBTPrintDialogFragment bindBTPrintDialogFragment = new BindBTPrintDialogFragment();
      bindBTPrintDialogFragment.show(getSupportFragmentManager(), "dialog");
      return;
    }

    final ProgressDialog progressDialog =
        ProgressDialog.show(this, getString(R.string.action_print),
            getString(R.string.wait_a_moment));
    Observable.from(mDataAdapter)
        .filter(new Func1<BaseData, Boolean>() {
          @Override public Boolean call(BaseData baseData) {
            return baseData.getType() != DataAdapter.DATA_TYPE_PHOTO_1
                && baseData.getType() != DataAdapter.DATA_TYPE_PHOTO_2;
          }
        })
        .flatMap(new Func1<BaseData, Observable<Boolean>>() {
          @Override public Observable<Boolean> call(BaseData baseData) {
            return btPrinter.print(baseData);
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Boolean>() {
          @Override public void onCompleted() {
            btPrinter.println("\n").subscribe();
            progressDialog.dismiss();
          }

          @Override public void onError(Throwable e) {
            Log.e("error", e.getMessage(), e);
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
    logoData.setBitmap(    getLogoBitMap());

    mDataAdapter.addData(logoData);
    if (!TextUtils.isEmpty(mSampleMaster.getDesc())) {
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

    Iterator<SampleProdLink> iterator = mSampleMaster.prodIterator();
    while (iterator.hasNext()) {
      SampleProdLink prod = iterator.next();
      TextData textData = new TextData(DataAdapter.DATA_TYPE_BARCODE);
      textData.setText(prod.ext + ": " + prod.prod_id + "  " + prod.getSpec());
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
      Bitmap comboBmp = Bitmap.createBitmap(410, 200, Bitmap.Config.RGB_565);
      Canvas c = new Canvas(comboBmp);
      Bitmap bmp1 = createQRBitMap(mSampleMaster.qrcode);
      Bitmap bmp2 = createQRBitMap("http://www.ledway.com.tw/uploads/sales_edge.apk");
      Paint paint = new Paint();
      paint.setColor(0xFFFFffFF);
      c.drawRect(0, 0, 410, 200, paint);
      paint.setColor(0xFF000000);
      paint.setTextSize(20);
      c.drawText("樣品追蹤", 0, 20, paint);
      c.drawBitmap(bmp1, 0, 25, null);
      c.drawText("下載app", 250, 20, paint);
      c.drawBitmap(bmp2, 250, 25, null);
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_QR_CODE);
      photoData.setBitmap(comboBmp);
      mDataAdapter.addData(photoData);
    } catch (WriterException e) {
      e.printStackTrace();
    }
  }

  private Bitmap createQRBitMap(String text) throws WriterException {
    QRCodeWriter writer = new QRCodeWriter();
    Map<EncodeHintType, Object> hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.MARGIN, 0); /* default = 4 */

    BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 160, 160, hints);
    int width = bitMatrix.getWidth();
    int height = bitMatrix.getHeight();
    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
      }
    }
    return bmp;
  }

  private Bitmap getLogoBitMap()  {
    final File cacheFile = new File(getCacheDir() + "logo.png");
    OkHttpClient client = new OkHttpClient();
    Request request =
        new Request.Builder().url("http://www.ledway.com.tw/uploads/sales_edge_banner.png").build();

    client.newCall(request).enqueue(new Callback() {
      @Override public void onFailure(Request request, IOException e) {
        System.out.println("request failed: " + e.getMessage());
      }

      @Override public void onResponse(Response response) {
        InputStream inputStream = null; // Read the data from the stream
        try {
          inputStream = response.body().byteStream();

          OutputStream outputStream = new FileOutputStream(cacheFile);
          byte[] buffer = new byte[1024];
          int length;
          while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
          }
          outputStream.flush();
          outputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    if (cacheFile.exists()) {
      InputStream inputStream = null;
      try {
        inputStream = new FileInputStream(cacheFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        return BitmapFactory.decodeStream(bufferedInputStream);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    return BitmapFactory.decodeResource(getResources(), R.drawable.logo);
  }
}
