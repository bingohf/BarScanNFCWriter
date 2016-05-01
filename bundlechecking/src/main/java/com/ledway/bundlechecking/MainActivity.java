package com.ledway.bundlechecking;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Vibrator;
import android.serialport.api.SerialPort;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.zkc.Service.CaptureService;
import java.util.regex.Pattern;

/**
 * Created by togb on 2016/5/1.
 */
public class MainActivity extends AppCompatActivity {
  @BindView(R.id.txt_barcode) EditText mEdtBarCode;
  private Vibrator vibrator;
  private NfcAdapter nfcAdapter;
  private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver() {

    @Override public void onReceive(Context context, Intent intent) {
      String text = intent.getExtras().getString("code");
      if (text.length() < 10) {
        Toast.makeText(MainActivity.this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
      }
      Pattern pattern = Pattern.compile("[^0-9a-zA-Z_ ]");
      if (!pattern.matcher(text).matches()) {
        mEdtBarCode.setText(text);
      } else {
        vibrator.vibrate(1000);
      }
    }
  };
  private String[][] mTechLists;
  private IntentFilter[] mFilters;
  private PendingIntent pendingIntent;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
    nfcAdapter = NfcAdapter.getDefaultAdapter(this);

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
  }

  @OnClick(R.id.btn_scan)
  void btnScanClock(){
    SerialPort.CleanBuffer();
    CaptureService.scanGpio.openScan();
  }
}
