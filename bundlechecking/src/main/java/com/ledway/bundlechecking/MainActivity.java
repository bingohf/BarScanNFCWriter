package com.ledway.bundlechecking;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Vibrator;
import android.serialport.api.SerialPort;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.zkc.Service.CaptureService;
import com.zkc.beep.ServiceBeepManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Created by togb on 2016/5/1.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  EditText mEdtBarCode;
  View mPrgLoading;
  TextView mTxtResponse;
  private byte keyA[] = {
      (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff
  };
  private NfcAdapter nfcAdapter;
  private PendingIntent pendingIntent;
  private IntentFilter[] mFilters;
  private String[][] mTechLists;
  private Intent intents;
  private ServiceBeepManager beepManager;
  private PublishSubject<String> mScanTimer = PublishSubject.create();
  private CompositeSubscription mSubscriptions = new CompositeSubscription();
  private long autoScanTimeStamp = System.currentTimeMillis();

  private CompositeSubscription subscriptions = new CompositeSubscription();
  private View mBtnClear;
  private ListView mListRecord;
  private Vibrator vibrator;
  private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver() {

    @Override public void onReceive(Context context, Intent intent) {
      String text = intent.getExtras().getString("code");
      if (text.length() < 10) {
        Toast.makeText(MainActivity.this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
      }
      Log.i(TAG, "MyBroadcastReceiver code:" + text);
      mEdtBarCode.setText(text);
      doQuery(text);
      mScanTimer.onNext("Receiver");
      Observable.just(true).delay(1,TimeUnit.SECONDS).subscribe(new Action1<Boolean>() {
        @Override public void call(Boolean aBoolean) {
          SerialPort.CleanBuffer();
          CaptureService.scanGpio.openScan();
          mScanTimer.onNext("Receiver-delay");
        }
      });
    }
  };

  private BroadcastReceiver systemBroadcastReceiver = new BroadcastReceiver() {
    @Override public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.v("action_test",action);
      if (action.equals("com.zkc.keycode")) {
        mScanTimer.onNext("scan_key");
        autoScanTimeStamp = System.currentTimeMillis();
      } else if (action.equals("android.intent.action.SCREEN_ON")) {
      } else if (action.equals("android.intent.action.SCREEN_OFF")) {
        autoScanTimeStamp = 0;
      } else if (action.equals("android.intent.action.ACTION_SHUTDOWN")) {
        autoScanTimeStamp = 0;
      }
    }
  };

  @Override protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(systemBroadcastReceiver);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mEdtBarCode = (EditText) findViewById(R.id.txt_barcode);
    mPrgLoading = findViewById(R.id.prg_loading);
    mTxtResponse = (TextView) findViewById(R.id.txt_response);
    findViewById(R.id.my_layout).requestFocus();

    //mListRecord.scrollTo(0,100000);
    nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

    pendingIntent = PendingIntent.getActivity(this, 0,
        new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
    ndef.addCategory("*/*");
    mFilters = new IntentFilter[] { ndef };// 过滤器
    mTechLists = new String[][] {
        new String[] { MifareClassic.class.getName() }, new String[] { NfcA.class.getName() }
    };// 允许扫描的标签类型

    Intent newIntent = new Intent(MainActivity.this, CaptureService.class);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startService(newIntent);
    subscriptions.add(Observable.create(new Observable.OnSubscribe<Object>() {
      @Override public void call(Subscriber<? super Object> subscriber) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.zkc.scancode");
        MainActivity.this.registerReceiver(scanBroadcastReceiver, intentFilter);
        subscriber.add(Subscriptions.create(new Action0() {
          @Override public void call() {
            MainActivity.this.unregisterReceiver(scanBroadcastReceiver);
          }
        }));
      }
    }).subscribe());
    findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        SerialPort.CleanBuffer();
        CaptureService.scanGpio.openScan();
        mScanTimer.onNext("Click");
        autoScanTimeStamp = System.currentTimeMillis();
      }
    });

    mEdtBarCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        doQuery(v.getText().toString());
        return false;
      }
    });

    findViewById(R.id.btn_camera_scan).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startActivityForResult(new Intent(MainActivity.this, FullScannerActivity.class), 1);
      }
    });



    mScanTimer.debounce(3, TimeUnit.SECONDS)
        .subscribe(new Action1<String>() {
          @Override public void call(String s) {
            Log.v("timer",s);
            long timeDelta = System.currentTimeMillis() - autoScanTimeStamp;
            if (timeDelta < 20000) {
              SerialPort.CleanBuffer();
              CaptureService.scanGpio.openScan();
              mScanTimer.onNext("Auto");
            }
          }
        });

    IntentFilter screenStatusIF = new IntentFilter();
    screenStatusIF.addAction(Intent.ACTION_SCREEN_ON);
    screenStatusIF.addAction(Intent.ACTION_SCREEN_OFF);
    screenStatusIF.addAction(Intent.ACTION_SHUTDOWN);
    screenStatusIF.addAction("com.zkc.keycode");
    registerReceiver(systemBroadcastReceiver,screenStatusIF);
  }

  @Override public void onBackPressed() {
    exitActivity();
  }


  private boolean validBarCode(String barcode)  {
    Pattern pattern = Pattern.compile("^[0-9a-zA-Z]*$");
    if (!pattern.matcher(barcode).matches()) {
      Toast.makeText(this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
      vibrator.vibrate(1000);
      return false;
    }
    return true;
  }


  private void doQuery(String barcode) {
    View view = this.getCurrentFocus();
    if (view != null) {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    if(!validBarCode(barcode)){
      return;
    }
    mTxtResponse.setText("");
    mPrgLoading.setVisibility(View.VISIBLE);
    MyProjectApi.getInstance()
        .getBarCodeDesc(barcode)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<String>() {
          @Override public void onCompleted() {
            mPrgLoading.setVisibility(View.GONE);
          }

          @Override public void onError(Throwable e) {
            mTxtResponse.setText(e.getMessage());
            mPrgLoading.setVisibility(View.GONE);
            e.printStackTrace();
          }

          @Override public void onNext(String s) {
            mTxtResponse.setText(s);
          }
        });
  }

  private void exitActivity() {
    new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.exit)
        .setMessage(R.string.exit_confirm)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          @Override public void onClick(DialogInterface dialog, int which) {

            CaptureService.scanGpio.closeScan();
            CaptureService.scanGpio.closePower();
            finish();
          }
        })
        .setNegativeButton(R.string.no, null)
        .show();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings:
        startActivityForResult(new Intent(this, AppPreferences.class), 2);
        break;
    }
    return true;
  }

  @Override protected void onResume() {
    super.onResume();
    if (nfcAdapter != null && nfcAdapter.isEnabled()) {
      nfcAdapter.enableForegroundDispatch(this, pendingIntent, mFilters, mTechLists);
      if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction())) {
        // 处理该intent
        intents = getIntent();
      }
    }
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
      // 处理该intent
      intents = intent;
      Tag tagFromIntent = intents.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      MifareClassic mfc = MifareClassic.get(tagFromIntent);

      try {
        mfc.connect();
        boolean auth = mfc.authenticateSectorWithKeyA(1, keyA);
        if (auth) {
          byte[] bytes = mfc.readBlock(4);
          String barcode = new String(bytes).trim();
          if (!TextUtils.isEmpty(barcode)) {
            mEdtBarCode.setText(barcode);
            doQuery(barcode);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 1 && resultCode == 1) {
      mEdtBarCode.setText(data.getStringExtra("barcode"));
      doQuery(mEdtBarCode.getText().toString());
    } else {
      MApp.getInstance().getSettings().reload();
    }
  }
}
