package com.ledway.scanmaster;

import android.app.Activity;
import android.app.Service;
import android.arch.lifecycle.MutableLiveData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.serialport.api.SerialPort;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.ledway.scanmaster.data.DBCommand;
import com.ledway.scanmaster.data.Settings;
import com.ledway.scanmaster.domain.InvalidBarCodeException;
import com.ledway.scanmaster.interfaces.IDGenerator;
import com.ledway.scanmaster.network.GroupRequest;
import com.ledway.scanmaster.network.GroupResponse;
import com.ledway.scanmaster.network.MyNetWork;
import com.ledway.scanmaster.network.RemoteMenu;
import com.ledway.scanmaster.network.SpGetMenuRequest;
import com.ledway.scanmaster.network.SpResponse;
import com.ledway.scanmaster.network.Sp_getBill_Request;
import com.ledway.scanmaster.network.Sp_getDetail_Request;
import com.ledway.scanmaster.utils.JsonUtils;
import com.zkc.Service.CaptureService;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by togb on 2018/1/6.
 */

public class ScanMasterFragment extends Fragment {
  final int GROUP_ID = 1001;
  private static final int REQUEST_GROUP = 1;
  private static final int REQUEST_BAR_CODE = 2;
  @Inject Settings settings;
  @Inject IDGenerator mIDGenerator;
  @BindView(R2.id.txt_bill_no) EditText mTxtBill;
  @BindView(R2.id.txt_barcode) EditText mTxtBarcode;
  @BindView(R2.id.prg_loading) View mLoading;
  @BindView(R2.id.web_response) WebView mWebResponse;
  @BindView(R2.id.btn_scan) Button mBtnScan;
  @BindView(R2.id.btn_camera_scan_barcode) ImageView mPAIcon;
  private Vibrator vibrator;
  private CompositeSubscription mSubscriptions = new CompositeSubscription();
  private DBCommand dbCommand = new DBCommand();
  private BroadcastReceiver sysBroadcastReceiver;
  private boolean mContinueScan;
  private EditText mCurrEdit;
  private BroadcastReceiver scanBroadcastReceiver;
  private String mMode ="Check";
  private MutableLiveData<RemoteMenu[]> menus = new MutableLiveData<>();

  public ScanMasterFragment(){
    setHasOptionsMenu(true);
  }
  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MApp) getActivity().getApplication()).getAppComponet().inject(this);
    vibrator = (Vibrator) getActivity().getApplication().getSystemService(Service.VIBRATOR_SERVICE);
    if(!TextUtils.isEmpty(settings.myTaxNo)){
      mMode = "In";
    }
    loadMenu();
    subscribeView();
  }

  private void subscribeView() {
    menus.observe(this, remoteMenus -> getActivity().invalidateOptionsMenu());
  }

  private void loadMenu() {
    SpGetMenuRequest request = new SpGetMenuRequest();
    request.line= settings.line;
    request.reader = settings.reader;
    request.MyTaxNo = settings.myTaxNo;
    request.pdaGuid = mIDGenerator.genID() +"~" + getLanguage();
    MyNetWork.getServiceApi().spGetScanMasterMenu(request).subscribeOn(Schedulers.io()).subscribe(
        new Subscriber<SpResponse>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {

          }

          @Override public void onNext(SpResponse response) {
            try {
              RemoteMenu[] tempMenus =
                  JsonUtils.Companion.fromJson(response.result[0].memotext, RemoteMenu[].class);
              menus.postValue(tempMenus);
            } catch (Exception e) {
              e.printStackTrace();
              Timber.e(e);
            }
          }
        });
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.activity_scan_master_main, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ButterKnife.bind(this, view);
    mWebResponse.getSettings().setJavaScriptEnabled(false);
    mTxtBarcode.requestFocus();
    listenKeyCode();
    receiveZkcCode();


    mSubscriptions.add(Observable.merge(RxTextView.editorActionEvents(mTxtBarcode),
        RxTextView.editorActionEvents(mTxtBill))
        // .observeOn(AndroidSchedulers.mainThread())
        .subscribe(actionEvent -> {
          onEditAction(actionEvent.view(), actionEvent.actionId(), actionEvent.keyEvent());
        }));
    Timber.v(mIDGenerator.genID());

    settingChanged();
    ScanMasterViewModel.getInstance().reader.observe(this, reader -> {
      settings.setReader(reader);
      settingChanged();
    });
  }

  @Override public void onStart() {
    super.onStart();
    CaptureService.scanGpio.openPower();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    mSubscriptions.clear();
    closeScan();
    getActivity().getApplication().unregisterReceiver(scanBroadcastReceiver);
    getActivity().getApplication().unregisterReceiver(sysBroadcastReceiver);
  }

  private void closeScan() {
    CaptureService.scanGpio.closeScan();
    CaptureService.scanGpio.closePower();
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
    getActivity().getApplication().registerReceiver(sysBroadcastReceiver, screenStatusIF);
  }

  private void hideInputMethod() {
    if (mCurrEdit != null) {
      InputMethodManager inputMethodManager =
          (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
      inputMethodManager.hideSoftInputFromWindow(mCurrEdit.getWindowToken(), 0);
    }
  }

  private void settingChanged() {
    String connectionStr =
        String.format("jdbc:jtds:sqlserver://%s;DatabaseName=%s;charset=UTF8", settings.getServer(),
            settings.getDb());
    Timber.v(connectionStr);
    dbCommand.setConnectionString(connectionStr);

    mBtnScan.setText("PDA#" + settings.getLine() + " / " + settings.getReader());
  }

  boolean onEditAction(TextView view, int actionId, KeyEvent keyEvent) {
    try {
      Timber.v("onEditAction");
      if (view.isEnabled() && (actionId == EditorInfo.IME_ACTION_SEARCH || (keyEvent.getAction()
          == KeyEvent.ACTION_UP && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
        if (view.getId() == R.id.txt_bill_no) {
          queryBill();
        } else if (view.getId() == R.id.txt_barcode) {
          queryBarCode();
        }
      }
    } catch (InvalidBarCodeException e) {
      Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
    }
    InputMethodManager inputMethodManager =
        (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    return true;
  }

  private void queryBill() throws InvalidBarCodeException {
    String billNo = mTxtBill.getText().toString();
   // validBarCode(billNo);
    mTxtBill.setEnabled(false);
    mLoading.setVisibility(View.VISIBLE);
    mWebResponse.setVisibility(View.GONE);
    Sp_getBill_Request request = new Sp_getBill_Request();
    request.billNo = billNo;
    request.line= settings.line;
    request.reader = settings.reader;
    request.type = mMode;
    request.MyTaxNo = settings.myTaxNo;
    request.pdaGuid = mIDGenerator.genID() +"~" + getLanguage();
    mSubscriptions.add(MyNetWork.getServiceApi().sp_getBill(request)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnUnsubscribe(() -> {
              mTxtBill.setEnabled(true);
              mLoading.setVisibility(View.GONE);
              mWebResponse.setVisibility(View.VISIBLE);
            })
            .subscribe(this::showResponse, this::showWarning));
  }

  private void showResponse(String s) {
    Timber.v(s);
    if(s != null) {
      mWebResponse.loadData(s, "text/html; charset=utf-8", "UTF-8");
      //mWebResponse.setBackgroundColor(Color.parseColor("#eeeeee"));
      alertWarning(s);
    }
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
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.warning)
          .setMessage(msg)
          .setPositiveButton(R.string.ok, null)
          .create()
          .show();
    }
  }

  private void queryBarCode() throws InvalidBarCodeException {
    String billNo = mTxtBill.getText().toString();
    String barCode = mTxtBarcode.getText().toString();
    //validBarCode(billNo);
   // validBarCode(barCode);
    mTxtBarcode.setEnabled(false);
    mLoading.setVisibility(View.VISIBLE);
    mWebResponse.setVisibility(View.GONE);
    Timber.v("start_query");

    Sp_getDetail_Request request = new Sp_getDetail_Request();
    request.line = settings.line;
    request.reader = settings.reader;
    request.billNo = billNo;
    request.detailNo = barCode;
    request.MyTaxNo = settings.myTaxNo;
    request.pdaGuid = mIDGenerator.genID() +"~" + getLanguage();
    request.type = mMode;
    mSubscriptions.add(MyNetWork.getServiceApi().sp_UpSampleDetail(request)
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

  private void showResponse(SpResponse spResponse) {
    showResponse(spResponse.result[0].memotext);
  }

  private void validBarCode(String barcode) throws InvalidBarCodeException {
    Pattern pattern = Pattern.compile("^[0-9a-zA-Z\\-\\#\\/\\~\\.]*$");
    if (!pattern.matcher(barcode).matches()) {
      throw new InvalidBarCodeException();
    }
  }

  private void showWarning(Throwable throwable) {
    Timber.e(throwable, throwable.getMessage());
    showWarning(throwable.getMessage());
  }

  private void showWarning(String message) {
    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    alertWarning(message);
  }

  private void receiveZkcCode() {
    scanBroadcastReceiver = new BroadcastReceiver() {

      @Override public void onReceive(Context context, Intent intent) {
        String text = intent.getExtras().getString("code");
        Timber.v(text);
        if (text.length() < 10) {
          Toast.makeText(getActivity(), R.string.invalid_barcode, Toast.LENGTH_LONG).show();
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
    getActivity().getApplication().registerReceiver(scanBroadcastReceiver, intentFilter);
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
      Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  protected void openScan() {
    SerialPort.CleanBuffer();
    CaptureService.scanGpio.openScan();
  }

  @OnFocusChange({ R2.id.txt_barcode, R2.id.txt_bill_no }) void onEditFocusChange(View view,
      boolean hasFocus) {
    if (hasFocus) {
      mCurrEdit = (EditText) view;
    }
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_scan_master_main, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
   if (id == R.id.action_set_group){
      startActivityForResult(new Intent("android.intent.action.full.scanner"), REQUEST_GROUP);
    }
    if(item.getGroupId() ==GROUP_ID) {
      item.setChecked(true);
      int index = item.getItemId();
      if(menus.getValue() != null) {
        mMode = menus.getValue()[index].menu_Label_Eng;
      }
    }
    if(getActivity() != null) {
      getActivity().invalidateOptionsMenu();
    }
    resetView();
    return super.onOptionsItemSelected(item);
  }

  private void resetView() {
    Drawable drawable = ContextCompat.getDrawable(getActivity(),
        "Photo".equals(mMode) ? R.drawable.ic_camera_black_24dp : R.drawable.ic_qrcode_scan_black_24dp);
    mPAIcon.setImageDrawable(drawable);
  }

  @Override public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_label).setTitle(mMode);
    menu.removeGroup(GROUP_ID);
    int i = 0;
    if(menus.getValue() != null) {
      for (RemoteMenu item : menus.getValue()) {
        menu.add(GROUP_ID, i++, i , item.menu_Label_Eng).setCheckable(true).setChecked(item.menu_Label_Eng.equals(mMode));
      }
    }
    menu.setGroupCheckable(GROUP_ID, true, true);
  }

  @OnClick(R2.id.btn_scan) void onBtnScanClick() {
    mContinueScan = true;
    openScan();
  }

  @OnClick(R2.id.btn_camera_scan_bill) void onBillCameraClick() {
    mCurrEdit = mTxtBill;
    mCurrEdit.requestFocus();
    startActivityForResult(new Intent("android.intent.action.full.scanner"), REQUEST_BAR_CODE);

  }

  @OnClick(R2.id.btn_camera_scan_barcode) void onBarCodeCameraClick() {
    mCurrEdit = mTxtBarcode;
    mCurrEdit.requestFocus();
    startActivityForResult(new Intent("android.intent.action.full.scanner"), REQUEST_BAR_CODE);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode){
      case REQUEST_BAR_CODE:{
        if(resultCode == Activity.RESULT_OK){
          String barCode = data.getStringExtra("barcode");
          receiveCode(barCode);
        }
        break;
      }
      case REQUEST_GROUP:{
        if(resultCode == Activity.RESULT_OK) {
          String barCode = data.getStringExtra("barcode");
          receiveGroup(barCode);
        }
        break;
      }
    }
  }

  private void receiveGroup(String barCode) {
    MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
    MaterialDialog progressDialog = builder.progress(true, 0).build();
    progressDialog.show();
    GroupRequest request = new GroupRequest();
    request.macNo = getArguments().getString("macNo");
    MyNetWork.getServiceApi().getGroup(barCode,request)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<GroupResponse>() {
      @Override public void onCompleted() {
        progressDialog.dismiss();
      }

      @Override public void onError(Throwable e) {
        progressDialog.dismiss();
        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
      }

      @Override public void onNext(GroupResponse groupResponse) {
        Toast.makeText(getActivity(),R.string.group_success, Toast.LENGTH_LONG).show();
        settings.setMyTaxNo(groupResponse.result[0].myTaxNo);
        settings.setLine(groupResponse.result[0].line);
        settingChanged();
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.app_name) + "(" + settings.myTaxNo +")");
        mMode ="In";
        getActivity().invalidateOptionsMenu();
      }
    });
  }

  private String getLanguage(){
    Locale locale = Locale.getDefault();
    return locale.getLanguage() +"_" + locale.getCountry();
  }
}
