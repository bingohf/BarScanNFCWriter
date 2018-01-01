package com.ledway.scanmaster;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Vibrator;
import android.serialport.api.SerialPort;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.ledway.scanmaster.data.DBCommand;
import com.ledway.scanmaster.data.Settings;
import com.ledway.scanmaster.domain.InvalidBarCodeException;
import com.ledway.scanmaster.interfaces.IDGenerator;
import com.ledway.scanmaster.nfc.GNfc;
import com.ledway.scanmaster.nfc.GNfcLoader;
import com.ledway.scanmaster.setting.AppPreferences;
import com.zkc.Service.CaptureService;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.inject.Inject;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.view.KeyEvent.ACTION_UP;

public class MainActivity extends AppCompatActivity {
  private static final int REQUEST_SET = 1;
  @Inject Settings settings;
  @Inject IDGenerator mIDGenerator;
  @BindView(R.id.txt_bill_no) EditText mTxtBill;
  @BindView(R.id.txt_barcode) EditText mTxtBarcode;
  @BindView(R.id.prg_loading) View mLoading;
  @BindView(R.id.web_response) WebView mWebResponse;
  @BindView(R.id.btn_scan) Button mBtnScan;
  private DBCommand dbCommand = new DBCommand();
  private CompositeSubscription mSubscriptions = new CompositeSubscription();
  private EditText mCurrEdit;
  private Vibrator vibrator;
  private BroadcastReceiver scanBroadcastReceiver;
  private BroadcastReceiver sysBroadcastReceiver;
  private NfcAdapter nfcAdapter;
  public boolean mContinueScan = false;
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    mWebResponse.getSettings().setJavaScriptEnabled(false);
    ((MApp) getApplication()).getAppComponet().inject(this);
    settingChanged();

    listenKeyCode();
    receiveZkcCode();

    nfcAdapter = NfcAdapter.getDefaultAdapter(this);

    mSubscriptions.add(Observable.merge(RxTextView.editorActionEvents(mTxtBarcode),
        RxTextView.editorActionEvents(mTxtBill))
        // .observeOn(AndroidSchedulers.mainThread())
        .subscribe(actionEvent -> {
          onEditAction(actionEvent.view(), actionEvent.actionId(), actionEvent.keyEvent());
        }));
    Timber.v(mIDGenerator.genID());
  }

  @Override protected void onStart() {
    super.onStart();
    CaptureService.scanGpio.openPower();
  }

  @Override protected void onStop() {
    super.onStop();
    closeScan();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mSubscriptions.clear();
    closeScan();
    unregisterReceiver(scanBroadcastReceiver);
    unregisterReceiver(sysBroadcastReceiver);
  }

  private void closeScan() {
    CaptureService.scanGpio.closeScan();
    CaptureService.scanGpio.closePower();
  }

  private void receiveZkcCode() {
    scanBroadcastReceiver = new BroadcastReceiver() {

      @Override public void onReceive(Context context, Intent intent) {
        String text = intent.getExtras().getString("code");
        Timber.v(text);
        if (text.length() < 10) {
          Toast.makeText(MainActivity.this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
        }
        receiveCode(text);
        if (mContinueScan) {
          mSubscriptions.add(Observable.timer(2, TimeUnit.SECONDS)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(l -> openScan()));
        }
      }
    };
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("com.zkc.scancode");
    registerReceiver(scanBroadcastReceiver, intentFilter);
  }

  private void listenKeyCode() {

    sysBroadcastReceiver = new BroadcastReceiver() {


      @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Timber.v(action);
        if (action.equals("com.zkc.keycode")) {
          mContinueScan = false;
          hideInputMethod();
        } else if (action.equals("android.intent.action.SCREEN_ON")) {
        } else if (action.equals("android.intent.action.SCREEN_OFF")) {
          closeScan();
        } else if (action.equals("android.intent.action.ACTION_SHUTDOWN")) {

        }
      }
    };
    IntentFilter screenStatusIF = new IntentFilter();
    screenStatusIF.addAction(Intent.ACTION_SCREEN_ON);
    screenStatusIF.addAction(Intent.ACTION_SCREEN_OFF);
    screenStatusIF.addAction(Intent.ACTION_SHUTDOWN);
    screenStatusIF.addAction("com.zkc.keycode");
    registerReceiver(sysBroadcastReceiver, screenStatusIF);
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_settings: {
        startActivityForResult(new Intent(this, AppPreferences.class), REQUEST_SET);
        break;
      }
    }
    return true;
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
    if (result != null) {
      if (result.getContents() != null) {
        String code = result.getContents();
        receiveCode(code);
      }
    } else {
      switch (requestCode) {
        case REQUEST_SET: {
          settingChanged();
          break;
        }
      }
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void receiveCode(String code) {

    hideInputMethod();
    try {
      if (mCurrEdit == null) {
        mTxtBarcode.requestFocus();
        mCurrEdit = mTxtBarcode;
      }
      if (mCurrEdit.isEnabled()) {
        mCurrEdit.setText(code);
        mCurrEdit.selectAll();
        if (mCurrEdit.getId() == R.id.txt_bill_no) {
          queryBill();
        } else if (mCurrEdit.getId() == R.id.txt_barcode) {
          queryBarCode();
        }
        mTxtBarcode.requestFocus();
      }
    } catch (InvalidBarCodeException e) {
      Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private void settingChanged() {
    String connectionStr =
        String.format("jdbc:jtds:sqlserver://%s;DatabaseName=%s;charset=UTF8", settings.getServer(),
            settings.getDb());
    Timber.v(connectionStr);
    dbCommand.setConnectionString(connectionStr);

    mBtnScan.setText("PDA#" +settings.getLine()+" / " + settings.getReader());

  }

  private void hideInputMethod() {
    if (mCurrEdit != null) {
      InputMethodManager inputMethodManager =
          (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
      inputMethodManager.hideSoftInputFromWindow(mCurrEdit.getWindowToken(), 0);
    }
  }

  private void queryBill() throws InvalidBarCodeException {
    String billNo = mTxtBill.getText().toString();
    validBarCode(billNo);
    mTxtBill.setEnabled(false);
    mLoading.setVisibility(View.VISIBLE);
    mWebResponse.setVisibility(View.GONE);
    mSubscriptions.add(
        dbCommand.rxExecute("{call sp_getBill(?,?,?,?)}", settings.getLine(), settings.getReader(),
            billNo)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnUnsubscribe(() -> {
              mTxtBill.setEnabled(true);
              mLoading.setVisibility(View.GONE);
              mWebResponse.setVisibility(View.VISIBLE);
            })
            .subscribe(this::showResponse, this::showWarning));
  }

  private void queryBarCode() throws InvalidBarCodeException {
    String billNo = mTxtBill.getText().toString();
    String barCode = mTxtBarcode.getText().toString();
    validBarCode(billNo);
    validBarCode(barCode);
    mTxtBarcode.setEnabled(false);
    mLoading.setVisibility(View.VISIBLE);
    mWebResponse.setVisibility(View.GONE);
    Timber.v("start_query");
    mSubscriptions.add(dbCommand.rxExecute("{call sp_getDetail(?,?,?,?,?,?)}", settings.getLine(),
        settings.getReader(), billNo, barCode, mIDGenerator.genID())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnUnsubscribe(() -> {
          mLoading.setVisibility(View.GONE);
          mWebResponse.setVisibility(View.VISIBLE);
          mTxtBarcode.setEnabled(true);
          Timber.v("end_query");
        })
        .subscribe(this::showResponse, this::showWarning));
  }

  private void validBarCode(String barcode) throws InvalidBarCodeException {
    Pattern pattern = Pattern.compile("^[0-9a-zA-Z\\-\\#\\/\\~\\.]*$");
    if (!pattern.matcher(barcode).matches()) {
      throw new InvalidBarCodeException();
    }
  }

  @Override public void onBackPressed() {
    exitActivity();
  }

  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
      Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      GNfc gnfc = GNfcLoader.load(tagFromIntent);

      try {
        gnfc.connect();
        String reader = gnfc.read();

        settings.setReader(reader);
        settingChanged();
        Toast.makeText(this,String.format("Set Reader to %s", reader) , Toast.LENGTH_LONG).show();
      } catch (IOException e) {
        e.printStackTrace();
        Timber.e(e, e.getMessage());
      }
    }
  }

  @Override protected void onResume() {
    super.onResume();
    if(nfcAdapter != null && nfcAdapter.isEnabled()) {
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
      IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
      ndef.addCategory("*/*");
      IntentFilter[] mFilters = new IntentFilter[] { ndef };// 过滤器
      nfcAdapter.enableForegroundDispatch(this, pendingIntent, mFilters, GNfcLoader.TechList);
    }
  }

  private void exitActivity() {
    new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.exit)
        .setMessage(R.string.exit_confirm)
        .setPositiveButton(R.string.yes, (dialog, which) -> finish())
        .setNegativeButton(R.string.no, null)
        .show();
  }

  @OnClick(R.id.btn_camera_scan_bill) void onBillCameraClick() {
    mCurrEdit = mTxtBill;
    mCurrEdit.requestFocus();
    new IntentIntegrator(this).initiateScan();
  }

  @OnClick(R.id.btn_camera_scan_barcode) void onBarCodeCameraClick() {
    mCurrEdit = mTxtBarcode;
    mCurrEdit.requestFocus();
    new IntentIntegrator(this).initiateScan();
  }

  @OnFocusChange({ R.id.txt_barcode, R.id.txt_bill_no }) void onEditFocusChange(View view,
      boolean hasFocus) {
    if (hasFocus) {
      mCurrEdit = (EditText) view;
    }
  }

  boolean onEditAction(TextView view, int actionId, KeyEvent keyEvent) {
    try {
      Timber.v("onEditAction");
      if (view.isEnabled() && (actionId == EditorInfo.IME_ACTION_SEARCH || (keyEvent.getAction()
          == ACTION_UP && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
        switch (view.getId()) {
          case R.id.txt_bill_no: {
            queryBill();
            break;
          }
          case R.id.txt_barcode: {
            queryBarCode();
            break;
          }
        }
      }
    } catch (InvalidBarCodeException e) {
      Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    }
    InputMethodManager inputMethodManager =
        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    return true;
  }

  @OnClick(R.id.btn_scan) void onBtnScanClick() {
    mContinueScan = true;
    openScan();
  }

  protected void openScan() {
    SerialPort.CleanBuffer();
    CaptureService.scanGpio.openScan();
  }

  private void showResponse(String s) {
    Timber.v(s);
    mWebResponse.loadData(s, "text/html; charset=utf-8", "UTF-8");
    //mWebResponse.setBackgroundColor(Color.parseColor("#eeeeee"));
    alertWarning(s);
  }

  private void alertWarning(String message) {
    String msg = message.replaceAll("^!+", "");
    int vibratorLen = message.length() - msg.length();
    if (vibratorLen > 0) {
      long[] vv = new long[vibratorLen * 2];
      for (int i = 0; i < vv.length / 2; ++i) {
        vv[i * 2] = 300;
        vv[i * 2 + 1] = 100;
      }
      vibrator.vibrate(vv, -1);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.warning)
          .setMessage(msg)
          .setPositiveButton(R.string.ok, null)
          .create()
          .show();
    }
  }

  private void showWarning(Throwable throwable) {
    Timber.e(throwable, throwable.getMessage());
    showWarning(throwable.getMessage());
  }

  private void showWarning(String message) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    alertWarning(message);
  }
}
