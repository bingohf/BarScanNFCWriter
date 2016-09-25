package com.ledway.btprinter;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import com.ledway.framework.FullScannerActivity;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/5/29.
 */
public class ItemDetailActivity extends AppCompatActivity {
  private final static int RESULT_TAKE_PHOTO_1 = 1;
  private final static int RESULT_TAKE_PHOTO_2 = 2;
  private final static int RESULT_CAMERA_QR_CODE = 3;
  private final static int RESULT_CAMERA_BAR_CODE = 4;
  private final static int RESULT_CAMERA_SHARE_TO = 5;
  public String mCurrentPhotoPath;
  private SampleMaster mSampleMaster;
  private RecyclerView mListViewProd;
  private List<Map<String, String>> mProdList = new ArrayList<>();
  private DataAdapter mDataAdapter;
  private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver() {

    @Override public void onReceive(Context context, Intent intent) {
      String text = intent.getExtras().getString("code");
      if (text.length() < 10) {
        Toast.makeText(ItemDetailActivity.this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
      }
      Pattern pattern = Pattern.compile("[^0-9a-zA-Z_ ]");
      if (!pattern.matcher(text).matches()) {
        appendBarCode(text);
      }
    }
  };

  private void appendBarCode(String text) {
    if (text.length() > 30) {
      text = text.substring(0, 30);
    }
    final ProgressDialog progressDialog =
        ProgressDialog.show(this, getString(R.string.loading), getString(R.string.wait_a_moment));
    mSampleMaster.addProd(text)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<SampleProdLink>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
          }

          @Override public void onError(Throwable e) {
            progressDialog.dismiss();
            Toast.makeText(ItemDetailActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
          }

          @Override public void onNext(SampleProdLink sampleProdLink) {
            TextData textData = new TextData(DataAdapter.DATA_TYPE_BARCODE);
            textData.setText(sampleProdLink.ext
                + ": "
                + sampleProdLink.prod_id
                + "  "
                + sampleProdLink.spec_desc);
            mDataAdapter.addData(textData);
            mListViewProd.scrollToPosition(mDataAdapter.getItemCount() - 1);
          }
        });
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSampleMaster = (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
    if (TextUtils.isEmpty(mSampleMaster.guid)) {
      mSampleMaster.guid =
          MApp.getApplication().getSystemInfo().getDeviceId() + "_" + System.currentTimeMillis();
    }
    if (TextUtils.isEmpty(mSampleMaster.dataFrom)) {
      mSampleMaster.dataFrom = MApp.getApplication().getSystemInfo().getBusinessCard();
    }
    mSampleMaster.reset();
    mSampleMaster.queryDetail();
    setContentView(R.layout.activity_item_detail);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    mListViewProd = (RecyclerView) findViewById(R.id.list_data);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("com.zkc.scancode");
    registerReceiver(scanBroadcastReceiver, intentFilter);
    setListView();
    setEvent();

    setView();
  }

  private void setView() {
    if (!TextUtils.isEmpty(mSampleMaster.getDesc())) {
      TextData textData = new TextData(DataAdapter.DATA_TYPE_MEMO);
      textData.setText(mSampleMaster.getDesc());
      mDataAdapter.addData(textData);
    }
    if (!TextUtils.isEmpty(mSampleMaster.getImage1())) {
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_PHOTO_1);
      photoData.setBitmapPath(mSampleMaster.getImage1());
      mDataAdapter.addData(photoData);
    }

    if (!TextUtils.isEmpty(mSampleMaster.getImage2())) {
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_PHOTO_2);
      photoData.setBitmapPath(mSampleMaster.getImage2());
      mDataAdapter.addData(photoData);
    }

    if (!TextUtils.isEmpty(mSampleMaster.shareToDeviceId)) {
      TextData textData = new TextData(DataAdapter.DATA_TYPE_SHARE_TO);
      textData.setText(mSampleMaster.shareToDeviceId);
      mDataAdapter.addData(textData);
    }
    Iterator<SampleProdLink> iterator = mSampleMaster.prodIterator();
    while (iterator.hasNext()) {
      SampleProdLink prod = iterator.next();
      TextData textData = new TextData(DataAdapter.DATA_TYPE_BARCODE);
      textData.setText(prod.ext + ": " + prod.prod_id + "  " + prod.spec_desc);
      mDataAdapter.addData(textData);
    }
  }

  private void setListView() {
    mDataAdapter = new DataAdapter(this);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    linearLayoutManager.setStackFromEnd(true);
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
    unregisterReceiver(scanBroadcastReceiver);
  }

  @Override public void onBackPressed() {
    if (mSampleMaster.isChanged()) {
      mSampleMaster.allSave();
    } else {
      setResult(-1);
    }
    super.onBackPressed();
  }

  private void setEvent() {
    findViewById(R.id.btn_scan_barcode).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        // SerialPort.CleanBuffer();
        // CaptureService.scanGpio.openScan();
        //  appendBarCode("C163003401001");
        startActivityForResult(new Intent(ItemDetailActivity.this, FullScannerActivity.class),
            RESULT_CAMERA_BAR_CODE);
      }
    });

    findViewById(R.id.btn_take_photo_1).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startTakePhoto(RESULT_TAKE_PHOTO_1);
      }
    });
    findViewById(R.id.btn_take_photo_2).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startTakePhoto(RESULT_TAKE_PHOTO_2);
      }
    });
    findViewById(R.id.btn_qr_code).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startActivityForResult(new Intent(ItemDetailActivity.this, FullScannerActivity.class),
            RESULT_CAMERA_QR_CODE);
      }
    });

    findViewById(R.id.btn_shareto).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startActivityForResult(new Intent(ItemDetailActivity.this, FullScannerActivity.class),
            RESULT_CAMERA_SHARE_TO);
      }
    });
    findViewById(R.id.btn_input_barcode).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ItemDetailActivity.this);
        builder.setTitle(R.string.input_barcode);
        final EditText input = new EditText(ItemDetailActivity.this);
        input.setHint(R.string.input_barcode);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(input);
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            if (!TextUtils.isEmpty(input.getText())) {
              appendBarCode(input.getText().toString());
            }
          }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
          }
        });
        builder.show();
      }
    });
  }

  private void startTakePhoto(int type) {

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      File photoFile = null;
      try {
        photoFile = new File(MApp.getApplication().getPicPath()
            + "/"
            + mSampleMaster.guid
            + "_type_"
            + type
            + ".jpeg");
        if (!photoFile.exists()) {
          photoFile.createNewFile();
        }
        mCurrentPhotoPath = photoFile.getAbsolutePath();
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (photoFile != null && photoFile.exists()) {
        Uri photoURI =
            FileProvider.getUriForFile(ItemDetailActivity.this, "com.ledway.btprinter.fileprovider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

        List<ResolveInfo> resolvedIntentActivities =
            getPackageManager().queryIntentActivities(takePictureIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
          String packageName = resolvedIntentInfo.activityInfo.packageName;

          grantUriPermission(packageName, photoURI,
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivityForResult(takePictureIntent, type);
      }
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home: {
        onBackPressed();
        break;
      }
      case R.id.action_upload: {
        if (mSampleMaster.isHasData()) {
          uploadAll();
        } else {
          Toast.makeText(this, R.string.pls_input_data, Toast.LENGTH_LONG).show();
        }
        break;
      }
      case R.id.action_print_preview: {
        if (mSampleMaster.isHasData()) {
          mSampleMaster.allSave();
          startActivity(new Intent(this, PrintPreviewActivity.class));
        } else {
          Toast.makeText(this, R.string.pls_input_data, Toast.LENGTH_LONG).show();
        }
        break;
      }
    }
    return true;
  }

  private void uploadRecord() {
    final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.save_record),
        getString(R.string.wait_a_moment));
    mSampleMaster.remoteSave()
        .subscribeOn(Schedulers.io())
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
    getMenuInflater().inflate(R.menu.item_detail_menu, menu);
    return true;
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_OK) {
      switch (requestCode) {
        case RESULT_TAKE_PHOTO_1:
        case RESULT_TAKE_PHOTO_2: {
          if (resultCode == RESULT_OK) {
            File f = new File(mCurrentPhotoPath);
            if (f.exists()) {
              if (f.length() < 1) {
                f.delete();
              }
            }
            if (f.exists()) {
              int type = RESULT_TAKE_PHOTO_2 == requestCode ? DataAdapter.DATA_TYPE_PHOTO_2
                  : DataAdapter.DATA_TYPE_PHOTO_1;
              PhotoData photoData = new PhotoData(type);
              photoData.setBitmapPath(f.getAbsolutePath());
              mDataAdapter.removeByType(type);
              mDataAdapter.addData(photoData);
              if (RESULT_TAKE_PHOTO_1 == requestCode) {
                mSampleMaster.setImage1(f.getAbsolutePath());
              } else {
                mSampleMaster.setImage2(f.getAbsolutePath());
              }
            }
          }
          break;
        }
        case RESULT_CAMERA_QR_CODE: {
          String qrcode = data.getStringExtra("barcode");
          TextData textData = new TextData(DataAdapter.DATA_TYPE_MEMO);
          textData.setText(qrcode);
          mSampleMaster.setDesc(qrcode);
          mDataAdapter.removeByType(DataAdapter.DATA_TYPE_MEMO);
          mDataAdapter.addData(textData);
          break;
        }
        case RESULT_CAMERA_SHARE_TO: {
          String qrcode = data.getStringExtra("barcode");
          mSampleMaster.setShareToDeviceId(qrcode);
          TextData textData = new TextData(DataAdapter.DATA_TYPE_SHARE_TO);
          textData.setText(qrcode);
          mDataAdapter.removeByType(DataAdapter.DATA_TYPE_SHARE_TO);
          mDataAdapter.addData(textData);
          break;
        }
        case RESULT_CAMERA_BAR_CODE: {
          String barcode = data.getStringExtra("barcode");
          appendBarCode(barcode);
          break;
        }
      }
    }
  }

  private void uploadAll() {
    final ProgressDialog progressDialog =
        ProgressDialog.show(this, getString(R.string.upload), getString(R.string.wait_a_moment),
            false);
    Observable.concat(Observable.just(mSampleMaster),
        Observable.from(RecordAdapter.getSingletonInstance()))
        .filter(new Func1<SampleMaster, Boolean>() {
          @Override public Boolean call(SampleMaster sampleMaster) {
            return !sampleMaster.isUploaded();
          }
        })
        .flatMap(new Func1<SampleMaster, Observable<SampleMaster>>() {
          @Override public Observable<SampleMaster> call(SampleMaster sampleMaster) {
            return sampleMaster.remoteSave();
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<SampleMaster>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
            RecordAdapter.getSingletonInstance().notifyDataSetChanged();
          }

          @Override public void onError(Throwable e) {
            progressDialog.dismiss();
            Log.e("upload_all", e.getMessage(), e);
            Toast.makeText(ItemDetailActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
          }

          @Override public void onNext(SampleMaster sampleMaster) {

          }
        });
  }
}

