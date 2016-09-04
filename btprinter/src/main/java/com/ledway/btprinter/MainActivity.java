package com.ledway.btprinter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.activeandroid.query.Select;
import com.example.android.common.view.SlidingTabLayout;
import com.ledway.btprinter.adapters.MyProfileViewPagerAdapter;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.fragments.BindBTPrintDialogFragment;
import com.ledway.btprinter.fragments.BusinessCardFragment;
import com.ledway.btprinter.fragments.MainFragment;
import com.ledway.btprinter.fragments.MyIDFragment;
import com.ledway.btprinter.fragments.NewVersionDialogFragment;
import com.ledway.btprinter.fragments.PagerFragment;
import com.ledway.btprinter.fragments.ReceiveSampleFragment;
import com.ledway.btprinter.fragments.ShareAppFragment;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.network.ApkVersionResponse;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.framework.RemoteDB;
import com.zkc.Service.CaptureService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity {
  private PublishSubject<Boolean> mSettingSubject = PublishSubject.create();
  private CompositeSubscription mSubscriptions = new CompositeSubscription();

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    Intent newIntent = new Intent(MainActivity.this, CaptureService.class);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startService(newIntent);
    mSettingSubject.subscribe(new Action1<Boolean>() {
      @Override public void call(Boolean aBoolean) {
        if (aBoolean) {
          reset();
        } else {
          AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
          builder.setTitle(R.string.invalid_setting)
              .setMessage(R.string.goto_setting)
              .setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                  Intent intent = new Intent(MainActivity.this, AppPreferences.class);
                  startActivityForResult(intent, AppConstants.REQUEST_TYPE_SETTING);
                }
              })
              .setCancelable(false);
          builder.create().show();
        }
      }
    });
    checkSetting();

    /*
    findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        SampleMaster sampleMaster = new SampleMaster();
        MApp.getApplication().getSession().put("current_data", sampleMaster);
        startActivityForResult(new Intent(MainActivity.this, ItemDetailActivity.class),
            AppConstants.REQUEST_TYPE_ADD_RECORD);
      }
    });
    */


    checkAgreement();

    checkVersion();

    ViewPager mViewPager = (ViewPager) findViewById(R.id.viewpager);
    mViewPager.setAdapter(new MyProfileViewPagerAdapter(getSupportFragmentManager(), new PagerFragment[]{new MainFragment(), new ReceiveSampleFragment()}));
    SlidingTabLayout mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
    mSlidingTabLayout.setViewPager(mViewPager);
  }

  @Override public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    mSubscriptions.clear();
    super.onSaveInstanceState(outState, outPersistentState);
  }

  private void checkVersion() {
    mSubscriptions.add(MyProjectApi.getInstance()
        .getLedwayService()
        .get_apk_version()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<ApkVersionResponse>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            Log.e("get_version", e.getMessage(), e);
          }

          @Override public void onNext(ApkVersionResponse apkVersionResponse) {
            if (apkVersionResponse.curVersion > BuildConfig.VERSION_CODE) {
              SharedPreferences sp = getSharedPreferences("upgrade", Context.MODE_PRIVATE);
              String currDate  = new SimpleDateFormat("yyyyMMdd").format(new Date());
              if (apkVersionResponse.minVersion > BuildConfig.VERSION_CODE || !sp.getBoolean(currDate, false) ) {
                NewVersionDialogFragment newVersionDialogFragment = new NewVersionDialogFragment();
                Bundle args = new Bundle();
                args.putString("url", "http://www.ledway.com.tw/uploads/sales_edge.apk");
                args.putString("apkName", "sales_edge_" + apkVersionResponse.curVersion  +".apk");
                args.putString("desc", apkVersionResponse.desc);
                args.putBoolean("cancelable", apkVersionResponse.minVersion > BuildConfig.VERSION_CODE);
                newVersionDialogFragment.setArguments(args);
                newVersionDialogFragment.setCancelable(false);

                newVersionDialogFragment.show(getSupportFragmentManager(), "dialog");
              }
            }
          }
        }));
  }

  private void checkAgreement() {
    SharedPreferences sp = getSharedPreferences("agreement", Context.MODE_PRIVATE);
    if (!sp.getBoolean("agree", false)) {
      startActivityForResult(new Intent(this, AgreementActivity.class),AppConstants. REQUEST_AGREEMENT);
    }
  }


  @Override protected void onDestroy() {
    super.onDestroy();
    RecordAdapter.setSingletonInstance(null);
  }

  private void reset() {
  }

  private void checkSetting() {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    String line = sp.getString("Line", "01");
    String reader = sp.getString("Reader", "01");
    String server = sp.getString("Server", "vip.ledway.com.tw");
    if (TextUtils.isEmpty(line) || TextUtils.isEmpty(reader) || TextUtils.isEmpty(server)) {
      mSettingSubject.onNext(false);
    } else {
      mSettingSubject.onNext(true);
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode,resultCode, data);
    SampleMaster currentData =
        (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
    switch (requestCode) {
      case AppConstants.REQUEST_TYPE_SETTING: {
        checkSetting();
        break;
      }
      case AppConstants.REQUEST_AGREEMENT: {
        if (resultCode == RESULT_OK) {
          SharedPreferences sp = getSharedPreferences("agreement", Context.MODE_PRIVATE);
          sp.edit().putBoolean("agree", true).apply();
        } else {
          finish();
        }
        break;
      }
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return  super.onCreateOptionsMenu(menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_bind_bt_printer: {
        showSetDialog();
        break;
      }
      case R.id.action_up_prod: {
        startActivity(new Intent(this, ProdListActivity.class));
        break;
      }
      case R.id.action_my_business_card: {
        startActivity(new Intent(this, BusinessCardActivity.class));
        break;
      }
    }
    return false;
  }

  private void showSetDialog() {
    BindBTPrintDialogFragment bindBTPrintDialogFragment = new BindBTPrintDialogFragment();
    bindBTPrintDialogFragment.show(getSupportFragmentManager(), "dialog");
  }


}
