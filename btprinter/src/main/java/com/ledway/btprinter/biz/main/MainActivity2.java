package com.ledway.btprinter.biz.main;

import android.app.Activity;
import android.app.PendingIntent;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.ledway.btprinter.AgreementActivity;
import com.ledway.btprinter.AppConstants;
import com.ledway.btprinter.AppPreferences;
import com.ledway.btprinter.BuildConfig;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.fragments.NewVersionDialogFragment;
import com.ledway.btprinter.network.ApkVersionResponse;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.scanmaster.ScanMasterFragment;
import com.ledway.scanmaster.ScanMasterViewModel;
import com.ledway.scanmaster.interfaces.MenuOpend;
import com.ledway.scanmaster.nfc.GNfc;
import com.ledway.scanmaster.nfc.GNfcLoader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static com.ledway.btprinter.AppConstants.REQUEST_AGREEMENT;

public class MainActivity2 extends AppCompatActivity {
  @BindView(R.id.viewPager) ViewPager mViewPager;
  @BindView(R.id.bottomNavigation) BottomNavigationView mBottomNav;

  Fragment scanMasterFragment = new ScanMasterFragment();
  Fragment[] fragments = new Fragment[] {
      new CombinFramgment(),  new ProductListFragment(),new MyAccountFragment(),scanMasterFragment,new  WebViewFragment()
  };
  private CompositeSubscription mSubscriptions = new CompositeSubscription();

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main2);

    String myTaxNo = getSharedPreferences("setting", Context.MODE_PRIVATE).getString("MyTaxNo","");
    if(!myTaxNo.isEmpty()){
      getSupportActionBar().setTitle(getString(R.string.app_name) + "(" + myTaxNo +")");
    }
    Bundle bundle = new Bundle();
    bundle.putString("macNo", MApp.getApplication()
            .getSystemInfo()
            .getDeviceId());
    scanMasterFragment.setArguments(bundle);

    initView();
/*    Intent newIntent = new Intent(this, CaptureService.class);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);*/
   // startService(newIntent);
    checkAgreement();
    doCheckSetting();
    checkVersion();
  }

  private void doCheckSetting() {
    if (!checkSetting()) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.invalid_setting)
          .setMessage(R.string.goto_setting)
          .setPositiveButton(R.string.setting, (dialog, which) -> {
            Intent intent = new Intent(this, AppPreferences.class);
            startActivityForResult(intent, AppConstants.REQUEST_TYPE_SETTING);
          })
          .setCancelable(false);
      builder.create().show();
    }
  }

  private boolean checkSetting() {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    String line = sp.getString("Line", "01");
    String reader = sp.getString("Reader", "01");
    String server = sp.getString("Server", "vip.ledway.com.tw");
    String cloudDataListUrl = sp.getString("cloudDataListUrl", "http://www.ledway.com.tw");
    sp.edit()
        .putString("Line", line)
        .putString("Reader", reader)
        .putString("Server", server)
        .putString("cloudDataListUrl", cloudDataListUrl)
        .apply();
    if ((TextUtils.isEmpty(line) || TextUtils.isEmpty(reader) || TextUtils.isEmpty(server))) {
      return false;
    }
    return true;
  }

  private void initView() {
    ButterKnife.bind(this);
    mViewPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
      @Override public Fragment getItem(int position) {
        return fragments[position];
      }

      @Override public int getCount() {
        return fragments.length;
      }
    });
    mBottomNav.setOnNavigationItemSelectedListener(item -> {
      mViewPager.setCurrentItem(item.getOrder(), false);
      return true;
    });
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override public void onPageSelected(int position) {
        mBottomNav.setSelectedItemId(mBottomNav.getMenu().getItem(position).getItemId());
        invalidateOptionsMenu();
      }

      @Override public void onPageScrollStateChanged(int state) {

      }
    });

    int lastIndex = getSharedPreferences("view", Context.MODE_PRIVATE).getInt("pagerIndex", 0);
    mViewPager.setCurrentItem(lastIndex);
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
              String currDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
              if (apkVersionResponse.minVersion > BuildConfig.VERSION_CODE || !sp.getBoolean(
                  currDate, false)) {
                NewVersionDialogFragment newVersionDialogFragment = new NewVersionDialogFragment();
                Bundle args = new Bundle();
                args.putString("url", "http://www.ledway.com.tw/uploads/sales_edge.apk");
                args.putString("apkName", "sales_edge_" + apkVersionResponse.curVersion + ".apk");
                args.putString("desc", apkVersionResponse.desc);
                args.putBoolean("cancelable",
                    apkVersionResponse.minVersion > BuildConfig.VERSION_CODE);
                newVersionDialogFragment.setArguments(args);
                newVersionDialogFragment.setCancelable(false);

                newVersionDialogFragment.show(getSupportFragmentManager(), "dialog");
              }
            }
          }
        }));
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mSubscriptions.clear();
    int currentIndex = mViewPager.getCurrentItem();
    getSharedPreferences("view", Context.MODE_PRIVATE).edit().putInt("pagerIndex", currentIndex).apply();
  }

  @Override protected void onResume() {
    super.onResume();
    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
    if(nfcAdapter != null && nfcAdapter.isEnabled()) {
      PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
      IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
      ndef.addCategory("*/*");
      IntentFilter[] mFilters = new IntentFilter[] { ndef };// 过滤器
      nfcAdapter.enableForegroundDispatch(this, pendingIntent, mFilters, GNfcLoader.TechList);
    }
  }

  private void checkAgreement() {
    SharedPreferences sp = getSharedPreferences("agreement", Context.MODE_PRIVATE);
    if (!sp.getBoolean("agree", false)) {
      startActivityForResult(new Intent(this, AgreementActivity.class), REQUEST_AGREEMENT);
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case AppConstants.REQUEST_TYPE_SETTING: {
        doCheckSetting();
        break;
      }
      case REQUEST_AGREEMENT: {
        if (resultCode != Activity.RESULT_OK) {
          finish();
        } else {
          SharedPreferences sp = getSharedPreferences("agreement", Context.MODE_PRIVATE);
          sp.edit().putBoolean("agree", true).apply();
        }
        break;
      }
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    //getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.action_scan_master){
      startActivity(new Intent(this, com.ledway.scanmaster.MainActivity.class));
    }
    return super.onOptionsItemSelected(item);

  }

  @Override public void onBackPressed() {
    new MaterialDialog.Builder(this).title(R.string.exit).content(R.string.are_you_sure_to_exit)
        .positiveText(R.string.yes)
        .negativeText(R.string.no)
        .onPositive((dialog, which) -> {
          finish();
        }).show();


  }



  @Override protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
      Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      GNfc gnfc = GNfcLoader.load(tagFromIntent);

      try {
        gnfc.connect();
        String reader = gnfc.read();
        getSharedPreferences("setting", Context.MODE_PRIVATE).edit().putString("reader", reader).apply();
        ScanMasterViewModel.getInstance().reader.setValue(reader);
        //   settings.setReader(reader);
        //  settingChanged();
        Toast.makeText(this,String.format("Set Reader to %s", reader) , Toast.LENGTH_LONG).show();
      } catch (IOException e) {
        e.printStackTrace();
        Timber.e(e, e.getMessage());
      }
    }
  }


  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if(event.getAction() == KeyEvent.ACTION_DOWN){
      if(keyCode == KeyEvent.KEYCODE_BACK){
        Fragment currentFragment = getCurrentFragment();

        boolean handled = false;
        if(currentFragment != null && currentFragment instanceof OnKeyPress &&   currentFragment.getLifecycle().getCurrentState().isAtLeast(
            Lifecycle.State.STARTED) ){
          handled =  ((OnKeyPress) currentFragment).onKeyDown(keyCode,event);
        }
        if(handled){
          return true;
        }else {
          return super.onKeyDown(keyCode, event);
        }
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override public boolean onMenuOpened(int featureId, Menu menu) {
    Fragment currentFragment = getCurrentFragment();
    if(currentFragment != null &&currentFragment instanceof MenuOpend &&   currentFragment.getLifecycle().getCurrentState().isAtLeast(
        Lifecycle.State.STARTED)){
      ((MenuOpend) currentFragment).menuOpened();
    }
    return super.onMenuOpened(featureId, menu);
  }


  private Fragment getCurrentFragment(){
    Fragment page = getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + mViewPager.getCurrentItem());
    return  page;
  }

}
