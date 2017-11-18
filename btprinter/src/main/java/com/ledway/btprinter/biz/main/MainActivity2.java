package com.ledway.btprinter.biz.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.ledway.btprinter.AppConstants;
import com.ledway.btprinter.AppPreferences;
import com.ledway.btprinter.R;
import com.zkc.Service.CaptureService;

public class MainActivity2 extends AppCompatActivity {
  @BindView(R.id.viewPager) ViewPager mViewPager;
  @BindView(R.id.bottomNavigation) BottomNavigationView mBottomNav;
  Class<Fragment>[] fragmentCls = new Class[] {
      SampleListFragment.class, ReceiveSampleListFragment.class, ProductListFragment.class
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main2);
    initView();
    Intent newIntent = new Intent(this, CaptureService.class);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startService(newIntent);
    doCheckSetting();
  }

  private void doCheckSetting() {
    if(!checkSetting()){
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

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode){
      case  AppConstants.REQUEST_TYPE_SETTING:{
        doCheckSetting();
        break;
      }
    }
  }

  private void initView() {
    ButterKnife.bind(this);
    mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
      @Override public Fragment getItem(int position) {
        try {
          return fragmentCls[position].newInstance();
        } catch (InstantiationException e) {
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
        return null;
      }

      @Override public int getCount() {
        return fragmentCls.length;
      }
    });
    mBottomNav.setOnNavigationItemSelectedListener(item -> {
      mViewPager.setCurrentItem(item.getOrder());
      return true;
    });
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override public void onPageSelected(int position) {
        mBottomNav.setSelectedItemId(mBottomNav.getMenu().getItem(position).getItemId());
      }

      @Override public void onPageScrollStateChanged(int state) {

      }
    });
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
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
}
