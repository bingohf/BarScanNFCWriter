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
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.zkc.Service.CaptureService;
import com.zkc.beep.ServiceBeepManager;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Created by togb on 2016/5/1.
 */
public class MainActivity extends AppCompatActivity {
  private static final String TAG = "MainActivity";
  private byte keyA[] = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
      (byte) 0xff, (byte) 0xff};

  private NfcAdapter nfcAdapter;
  private PendingIntent pendingIntent;
  private IntentFilter[] mFilters;
  private String[][] mTechLists;
  private Intent intents;
  EditText mEdtBarCode;
  View mPrgLoading;
  TextView mTxtResponse;
  private ServiceBeepManager beepManager;
  private CompositeSubscription subscriptions = new CompositeSubscription();
  private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver(){

    @Override
    public void onReceive(Context context, Intent intent) {
      String text = intent.getExtras().getString("code");
      if (text.length()<10){
        Toast.makeText(MainActivity.this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
      }
      Log.i(TAG, "MyBroadcastReceiver code:" + text);
      Pattern pattern = Pattern.compile("[^0-9a-zA-Z_ ]");
      if(!pattern.matcher(text).matches()) {
        mEdtBarCode.setText(text);
      }else{
        vibrator.vibrate(1000);
      }
    }
  };



  private View mBtnClear;
  private ListView mListRecord;
  private Vibrator vibrator;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mEdtBarCode = (EditText) findViewById(R.id.txt_barcode);
    mPrgLoading = findViewById(R.id.prg_loading);
    mTxtResponse = (TextView) findViewById(R.id.txt_response);
    findViewById(R.id.my_layout).requestFocus();

    //mListRecord.scrollTo(0,100000);
    nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    vibrator=(Vibrator)getSystemService(Service.VIBRATOR_SERVICE);


    pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
        getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
    ndef.addCategory("*/*");
    mFilters = new IntentFilter[] { ndef };// 过滤器
    mTechLists = new String[][] {
        new String[] { MifareClassic.class.getName() },
        new String[] { NfcA.class.getName() } };// 允许扫描的标签类型

    Intent newIntent = new Intent(MainActivity.this, CaptureService.class);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP );
    startService(newIntent);
    subscriptions.add(Observable.create(new Observable.OnSubscribe<Object>() {
      @Override
      public void call(Subscriber<? super Object> subscriber) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.zkc.scancode");
        MainActivity.this.registerReceiver(scanBroadcastReceiver, intentFilter);
        subscriber.add(Subscriptions.create(new Action0() {
          @Override
          public void call() {
            MainActivity.this.unregisterReceiver(scanBroadcastReceiver);
          }
        }));
      }
    }).subscribe());
    findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        SerialPort.CleanBuffer();
        CaptureService.scanGpio.openScan();
      }
    });

    mEdtBarCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        doQuery(v.getText().toString());
        return  false;
      }
    });
  }

  @Override public void onBackPressed() {
    exitActivity();
  }

  private void doQuery(String barcode) {

    View view = this.getCurrentFocus();
    if (view != null) {
      InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    mPrgLoading.setVisibility(View.VISIBLE);
    MyProjectApi.getInstance().getBarCodeDesc(barcode)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<String>() {
          @Override public void onCompleted() {
            mPrgLoading.setVisibility(View.GONE);
          }

          @Override public void onError(Throwable e) {
            mTxtResponse.setText(e.getMessage());
            e.printStackTrace();
          }

          @Override public void onNext(String s) {
            mTxtResponse.setText(s);
          }
        });
  }

  private void exitActivity() {
    new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.exit)
        .setMessage(R.string.exit_confirm)
        .setPositiveButton(R.string.yes,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog,
                  int which) {

                CaptureService.scanGpio.closeScan();
                CaptureService.scanGpio.closePower();
                finish();
              }
            }).setNegativeButton(R.string.no, null).show();
  }





}
