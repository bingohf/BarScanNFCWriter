package com.ledway.scanmaster;

import android.app.Activity;
import android.app.Service;
import androidx.lifecycle.MutableLiveData;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import butterknife.OnTextChanged;
import com.afollestad.materialdialogs.MaterialDialog;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.ledway.scanmaster.data.DBCommand;
import com.ledway.scanmaster.data.Settings;
import com.ledway.scanmaster.domain.InvalidBarCodeException;
import com.ledway.scanmaster.interfaces.IDGenerator;
import com.ledway.scanmaster.interfaces.MenuOpend;
import com.ledway.scanmaster.model.Resource;
import com.ledway.scanmaster.network.JoinGroupItem;
import com.ledway.scanmaster.network.MyNetWork;
import com.ledway.scanmaster.network.RemoteMenu;
import com.ledway.scanmaster.network.ServiceApi;
import com.ledway.scanmaster.network.SpGetMenuRequest;
import com.ledway.scanmaster.network.SpMaProcessRequest;
import com.ledway.scanmaster.network.SpResponse;
import com.ledway.scanmaster.network.Sp_getBill_Request;
import com.ledway.scanmaster.utils.ContextUtils;
import com.ledway.scanmaster.utils.IOUtil;
import com.ledway.scanmaster.utils.JsonUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.inject.Inject;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by togb on 2018/1/6.
 */

public class ScanMasterFragment2 extends Fragment implements MenuOpend {
  static final String SP_SM ="SM";
  static final int REQUEST_TAKE_PHOTO = 3;
  private static final int REQUEST_GROUP = 1;
  private static final int REQUEST_BAR_CODE = 2;
  private static ServiceApi serviceApi;
  final int GROUP_ID = 1001;
  boolean isAutoReset = false;
  @Inject Settings settings;
  @Inject IDGenerator mIDGenerator;
  @BindView(R2.id.txt_bill_no) EditText mTxtBill;
  @BindView(R2.id.txt_barcode) EditText mTxtBarcode;
  @BindView(R2.id.prg_loading) View mLoading;
  @BindView(R2.id.web_response) WebView mWebResponse;
  @BindView(R2.id.btn_scan) Button mBtnScan;
  @BindView(R2.id.edt_qty) EditText mEdtQty;
  @BindView(R2.id.edt_lotno) EditText mEdtLotNo;
  @BindView(R2.id.edt_memo) EditText mEdtMemo;
  @BindView(R2.id.btn_camera_scan_barcode) ImageView mPAIcon;
  @BindView(R2.id.calc_clear_txt_barcode) View mBtnClearBarCode;
  @BindView(R2.id.calc_clear_txt_bill_no) View mBtnClearBillNo;
  @BindView(R2.id.image) ImageView mImageView;
  String mCurrentPhotoPath;

  private Vibrator vibrator;
  private CompositeSubscription mSubscriptions = new CompositeSubscription();
  private DBCommand dbCommand = new DBCommand();
  private BroadcastReceiver sysBroadcastReceiver;
  private boolean mContinueScan;
  private EditText mCurrEdit;
  private BroadcastReceiver scanBroadcastReceiver;
  private String mMode = "Check";
  private int mModeIndex = 0;
  private MutableLiveData<Resource<RemoteMenu[]>> menus = new MutableLiveData<>();

  public ScanMasterFragment2() {
    setHasOptionsMenu(true);
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

  boolean onEditAction(TextView view, int actionId, KeyEvent keyEvent) {
    try {
      Timber.v("onEditAction");
      if (view.isEnabled() && (actionId == EditorInfo.IME_ACTION_DONE
          || actionId == EditorInfo.IME_ACTION_SEARCH || (keyEvent != null && keyEvent.getAction()
          == KeyEvent.ACTION_UP && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER))) {
        if (view.getId() == R.id.txt_bill_no) {
          //queryBill();
          mEdtLotNo.requestFocus();

        } else if (view.getId() == R.id.txt_barcode) {
          queryBarCode();
        }
      }
    } catch (InvalidBarCodeException e) {
      Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
    }
/*    InputMethodManager inputMethodManager =
        (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    if(inputMethodManager != null) {
      inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }*/
    return false;
  }

  private void showResponse(SpResponse spResponse) {
    showResponse(spResponse.result[0].memotext);
    mImageView.setImageDrawable(null);
    mCurrentPhotoPath = "";
    if(isAutoReset){
      mEdtLotNo.setText("");
      mEdtMemo.setText("");
      mEdtQty.setText("");
      mTxtBill.setText("");
      mTxtBarcode.setText("");
    }
    //Toast.makeText(getContext(),spResponse.result[0].memotext,Toast.LENGTH_LONG).show();
  }

  private void showResponse(String s) {
    Timber.v(s);
    if (s != null) {
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
      if(vibratorLen == 3 && mCurrEdit != null){
        mCurrEdit.setText("");
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
    if(message == null) message = "";
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

  @OnFocusChange({ R2.id.txt_barcode, R2.id.txt_bill_no }) void onEditFocusChange(View view,
      boolean hasFocus) {
    if (hasFocus) {
      mCurrEdit = (EditText) view;
    }
  }

  @OnClick(R2.id.btn_scan) void onBtnScanClick() {
    mContinueScan = true;
    openScan();
  }

  protected void openScan() {
/*    SerialPort.CleanBuffer();
    CaptureService.scanGpio.openScan();*/
  }

  @OnClick(R2.id.btn_camera_scan_bill) void onBillCameraClick() {
    mCurrEdit = mTxtBill;
    mCurrEdit.requestFocus();
    startActivityForResult(new Intent("android.intent.action.full.scanner").setPackage(getActivity().getPackageName()), REQUEST_BAR_CODE);
  }

  @OnTextChanged(R2.id.txt_bill_no) void onBillNoChanged(){
    mBtnClearBillNo.setVisibility(mTxtBill.getText().length()>0 ? View.VISIBLE:View.GONE);
  }


  @OnClick(R2.id.calc_clear_txt_bill_no) void onBtnClearTextBillNo(){
    mTxtBill.setText("");
  }
  @OnTextChanged(R2.id.txt_barcode) void onBarCodeChanged(){
    mBtnClearBarCode.setVisibility(mTxtBarcode.getText().length()>0 ? View.VISIBLE:View.GONE);
  }


  @OnClick(R2.id.calc_clear_txt_barcode) void onBtnClearTextBarCode(){
    mTxtBarcode.setText("");
  }

  @OnClick(R2.id.btn_camera_scan_barcode) void onBarCodeCameraClick() {
    mCurrEdit = mTxtBarcode;
    mCurrEdit.requestFocus();
    startActivityForResult(new Intent("android.intent.action.full.scanner").setPackage(getActivity().getPackageName()), REQUEST_BAR_CODE);
  }

  @OnClick(R2.id.btn_photo) void  onBtnPhotoClick(){
    dispatchTakePictureIntent();
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_BAR_CODE: {
        if (resultCode == Activity.RESULT_OK) {
          String barCode = data.getStringExtra("barcode");
          receiveCode(barCode);
        }
        break;
      }
      case REQUEST_GROUP: {
        if (resultCode == Activity.RESULT_OK) {
          String barCode = data.getStringExtra("barcode");
          receiveGroup(barCode);
        }
        break;
      }
      case REQUEST_TAKE_PHOTO:{
        if (resultCode == Activity.RESULT_OK) {
          try {
            mImageView.setVisibility(View.VISIBLE);
            mWebResponse.setVisibility(View.GONE);
            //Picasso.with(getActivity()).load(mCurrentPhotoPath).into(mImageView);
            Bitmap bitmap = IOUtil.loadImage(mCurrentPhotoPath, mImageView.getMeasuredWidth(), mImageView.getMeasuredHeight());
            mImageView.setImageBitmap(bitmap);
           // mImageView.setImageResource(R.drawable.ic_barcode_black_18dp);
       //     queryBill(mCurrentPhotoPath);
          } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
          }
        }
        break;
      }
    }
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MApp) getActivity().getApplication()).getAppComponet().inject(this);
    vibrator = (Vibrator) getActivity().getApplication().getSystemService(Service.VIBRATOR_SERVICE);
    if (!TextUtils.isEmpty(settings.myTaxNo)) {
      mMode = "In";
    }

    SharedPreferences sp = getContext().getSharedPreferences(SP_SM, Context.MODE_PRIVATE);
    isAutoReset = sp.getBoolean("isAutoReset",false);
    String company = sp.getString("mt_company", "");
  //  loadMenu();
    subscribeView();
    buildApi();
    ((AppCompatActivity) getActivity()).getSupportActionBar()
        .setTitle(company + " " + getString(R.string.app_name));
  }
  private static void buildApi(){
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS);
    builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
    OkHttpClient client = builder.build();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://www.ledway.com.tw:8089/datasnap/rest/TLwDataModule/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .client(client)
        .build();
    serviceApi = retrofit.create(ServiceApi.class);
  }
  private void subscribeView() {
    menus.observe(this, remoteMenus -> {
      if(getActivity() == null || remoteMenus == null){
        return;
      }
      switch (remoteMenus.status){
        case ERROR:{
          Toast.makeText(getActivity(), remoteMenus.message, Toast.LENGTH_LONG).show();
          break;
        }
        case SUCCESS:{
          getActivity().invalidateOptionsMenu();
        }
      }
    });
  }

  private void loadMenu() {
    SpGetMenuRequest request = new SpGetMenuRequest();
    request.line = settings.line;
    request.reader = settings.reader;
    request.MyTaxNo = settings.myTaxNo;
    request.pdaGuid = mIDGenerator.genID() + "~" + getLanguage();
    MyNetWork.getServiceApi()
        .spGetScanMasterMenu_MT(request)
        .subscribeOn(Schedulers.io())
        .subscribe(new Subscriber<SpResponse>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            menus.postValue(Resource.error(ContextUtils.getMessage(e), null));
          }

          @Override public void onNext(SpResponse response) {
            try {
              RemoteMenu[] tempMenus =
                  JsonUtils.Companion.fromJson(response.result[0].memotext, RemoteMenu[].class);
              menus.postValue(Resource.success(tempMenus));
            } catch (Exception e) {
              e.printStackTrace();
              Timber.e(e);
            }
          }
        });
  }

  private String getLanguage() {
    Locale locale = Locale.getDefault();
    return locale.getLanguage() + "_" + locale.getCountry();
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.activity_scan_master_main2, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ButterKnife.bind(this, view);
    mWebResponse.getSettings().setJavaScriptEnabled(false);
   // mTxtBarcode.requestFocus();
    listenKeyCode();
    receiveZkcCode();

    mSubscriptions.add(RxTextView.editorActionEvents(mTxtBarcode)
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

    if(savedInstanceState == null) {
      try {
        //queryBill(null, "Hello");
      } catch (Exception e) {
        e.printStackTrace();
        Timber.e(e);
      }
    }
  }

  @Override public void onStart() {
    super.onStart();
  //  CaptureService.scanGpio.openPower();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    mSubscriptions.clear();
    closeScan();
    getActivity().getApplication().unregisterReceiver(scanBroadcastReceiver);
    getActivity().getApplication().unregisterReceiver(sysBroadcastReceiver);
  }

  private void closeScan() {
   // CaptureService.scanGpio.closeScan();
   /// CaptureService.scanGpio.closePower();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_scan_master_main2, menu);
  }

  @Override public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_reset).setChecked(isAutoReset);
 /*   menu.removeGroup(GROUP_ID);
    menu.findItem(R.id.action_label).setVisible(false);
    int i = 0;
    if (menus.getValue() != null && menus.getValue().data != null) {
      for (RemoteMenu item : menus.getValue().data) {
        if(TextUtils.isEmpty(item.menu_name)){
          item.menu_name = item.menu_Label_Eng;
        }
        boolean isChecked = item.menu_Label_Eng.equals(mMode);
        if(isChecked){
          mModeIndex = i;
          menu.findItem(R.id.action_label).setVisible(true);
          menu.findItem(R.id.action_label).setTitle(item.menu_name);
        }
        menu.add(GROUP_ID, i, i, item.menu_name)
            .setCheckable(true)
            .setChecked(isChecked);
        ++i;
      }
    }
    menu.setGroupCheckable(GROUP_ID, true, true);*/
  }



  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if(id == R.id.action_reset){
      isAutoReset = !isAutoReset;
      getContext().getSharedPreferences(SP_SM, Context.MODE_PRIVATE).edit().putBoolean("isAutoReset",isAutoReset).apply();
    }
    if (id == R.id.action_set_group) {
      startActivityForResult(new Intent("android.intent.action.full.scanner").setPackage(getActivity().getPackageName()), REQUEST_GROUP);
    }
    if(id == R.id.action_about){
      startActivity(new Intent(getContext(), AboutActivity.class));
    }
    if (item.getGroupId() == GROUP_ID) {
      item.setChecked(true);
      int index = item.getItemId();
      if (menus.getValue() != null && menus.getValue().data != null) {
        mMode = menus.getValue().data[index].menu_Label_Eng;
      }
    }
    if (getActivity() != null) {
      getActivity().invalidateOptionsMenu();
    }
    return super.onOptionsItemSelected(item);
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
        //  queryBill();
        } else if (mCurrEdit.getId() == R.id.txt_barcode) {
          queryBarCode();
        }
     //   mTxtBarcode.requestFocus();
      }
    } catch (InvalidBarCodeException e) {
      Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  private void hideInputMethod() {
    if (mCurrEdit != null) {
      InputMethodManager inputMethodManager =
          (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
      inputMethodManager.hideSoftInputFromWindow(mCurrEdit.getWindowToken(), 0);
    }
  }

  private void queryBill() throws InvalidBarCodeException {
    queryBill(null);
  }

  private void queryBill(String photoPath) throws InvalidBarCodeException {
    queryBill(photoPath, mMode);
  }

  private void queryBill(String photoPath, String type) throws InvalidBarCodeException {
    String billNo = mTxtBill.getText().toString();
    // validBarCode(billNo);
    mTxtBill.setEnabled(false);
    mLoading.setVisibility(View.VISIBLE);
    mWebResponse.setVisibility(View.GONE);
    Sp_getBill_Request request = new Sp_getBill_Request();
    request.billNo = billNo;
    request.line = settings.line;
    request.reader = settings.reader;
    request.type = type;
    request.MyTaxNo = settings.myTaxNo;
    request.pdaGuid = mIDGenerator.genID() + "~" + getLanguage();
    Observable<SpResponse> apiOb = MyNetWork.getServiceApi().sp_getBill(request);
    if(!TextUtils.isEmpty(photoPath)){
      File photoFile = new File(mCurrentPhotoPath);
      if(photoFile.exists()){
        try {
          Bitmap bitmap512 = IOUtil.scaleCrop(photoFile, 512);
          request.graphic = IOUtil.bitmapToBase64(bitmap512, 70);
          bitmap512.recycle();
          Bitmap bitmap128 = IOUtil.scaleCrop(photoFile, 128);
          request.graphic2 = IOUtil.bitmapToBase64(bitmap128, 70);
          bitmap128.recycle();
          apiOb = MyNetWork.getServiceApi().sp_getBill_photo(request);
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
    }
    mSubscriptions.add(apiOb
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
    String process = mTxtBarcode.getText().toString();
    String lotno = mEdtLotNo.getText().toString();
    String qty = mEdtQty.getText().toString();

    if(TextUtils.isEmpty(billNo)){
      Toast.makeText(getContext(),"請輸入稽核單號",Toast.LENGTH_LONG).show();
      return;
    }
    if(TextUtils.isEmpty(lotno)){
      Toast.makeText(getContext(),"請輸入缸號",Toast.LENGTH_LONG).show();
      return;
    }
    if(TextUtils.isEmpty(qty)){
      Toast.makeText(getContext(),"請輸入數量",Toast.LENGTH_LONG).show();
      return;
    }
    if(TextUtils.isEmpty(process)){
      Toast.makeText(getContext(),"請輸入進度",Toast.LENGTH_LONG).show();
      return;
    }
    //validBarCode(billNo);
    // validBarCode(barCode);
    mTxtBarcode.setEnabled(false);
    mLoading.setVisibility(View.VISIBLE);
    mWebResponse.setVisibility(View.GONE);
    Timber.v("start_query");

    SpMaProcessRequest request = new SpMaProcessRequest();
    request.line = settings.line;
    request.reader = settings.reader;
    request.mytaxno = settings.myTaxNo;
    request.process = process;
    request.lotno = lotno;
    request.lotq = Integer.parseInt(qty);
    request.color_stuffno = mTxtBill.getText().toString();
    request.memo = mEdtMemo.getText().toString();
    if (!TextUtils.isEmpty(mCurrentPhotoPath)) {
      Bitmap bitmap512 = null;
      try {
        bitmap512 = IOUtil.scaleCrop(new File(mCurrentPhotoPath), 512);
        request.pic = IOUtil.bitmapToBase64(bitmap512, 70);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }

    mSubscriptions.add(serviceApi
        .sp_MaProcessScan(request)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnUnsubscribe(() -> {
          mLoading.setVisibility(View.GONE);
          mWebResponse.setVisibility(View.VISIBLE);
          mTxtBarcode.setEnabled(true);
        //  mTxtBarcode.setText("");
          Timber.v("end_query");
        })
        .subscribe(this::showResponse, this::showWarning));
  }

  private void receiveGroup(String barCode) {
    MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
    MaterialDialog progressDialog = builder.progress(true, 0).build();
    progressDialog.show();
    String macno =  android.provider.Settings.Secure.getString(getContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    MyNetWork.getServiceApi()
        .sp_join_group(macno,barCode)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<List<JoinGroupItem>>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
          }

          @Override public void onError(Throwable e) {
            progressDialog.dismiss();
            if (e instanceof HttpException){
              HttpException error = (HttpException) e;
              String message = null;
              try {
                message = error.response().errorBody().string();
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
              } catch (IOException e1) {
                e1.printStackTrace();
              }

            }

          }

          @Override public void onNext(List<JoinGroupItem> groupResponse) {
            JoinGroupItem item = groupResponse.get(0);
            Toast.makeText(getActivity(), R.string.group_success, Toast.LENGTH_LONG).show();
            settings.setMyTaxNo(item.getMyTaxNo());
            settings.setLine(item.getLine());
            getActivity().getSharedPreferences(SP_SM,Context.MODE_PRIVATE).edit()
                .putString("mt_company",item.getMt_company())
                .putString("mt_port", item.getMt_port())
                .putString("mt_server", item.getMt_server())
                .apply();
            settingChanged();
            ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setTitle(item.getMt_company()+ " " + getString(R.string.app_name));
            mMode = "In";
            getActivity().invalidateOptionsMenu();
          }
        });
  }

  @Override public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    mWebResponse.saveState(outState);
  }

  @Override public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    mWebResponse.restoreState(savedInstanceState);
  }

  private void settingChanged() {
    String connectionStr =
        String.format("jdbc:jtds:sqlserver://%s;DatabaseName=%s;charset=UTF8", settings.getServer(),
            settings.getDb());
    Timber.v(connectionStr);
    dbCommand.setConnectionString(connectionStr);

    mBtnScan.setText("PDA#" + settings.getLine() + " / " + settings.myTaxNo);
  }

  private void dispatchTakePictureIntent() {
    Context context = getActivity();
    if(context == null) return;
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    // Ensure that there's a camera activity to handle the intent
    if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
      // Create the File where the photo should go
      File photoFile = null;
      try {
        photoFile = createImageFile();
      } catch (IOException ex) {
        Timber.e(ex);
      }
      // Continue only if the File was successfully created
      if (photoFile != null) {


        Uri photoURI = FileProvider.getUriForFile(getActivity().getApplicationContext(),
            getContext().getPackageName() + ".provider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
      }
    }
  }

  private File createImageFile() throws IOException {
    // Create an image file name
    Context  context = getActivity();
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    String imageFileName = "JPEG_" + timeStamp + "_";
    File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    File image = File.createTempFile(imageFileName,  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */);

    // Save a file: path for use with ACTION_VIEW intents
    mCurrentPhotoPath = image.getAbsolutePath();
    return image;
  }

  @Override public void menuOpened() {
    if(menus.getValue() == null || menus.getValue().data == null) {
    //  loadMenu();
    }
  }
}
