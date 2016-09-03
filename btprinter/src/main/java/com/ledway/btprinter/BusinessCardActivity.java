package com.ledway.btprinter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import com.example.android.common.view.SlidingTabLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.ledway.btprinter.adapters.MyProfileViewPagerAdapter;
import com.ledway.btprinter.fragments.BusinessCardFragment;
import com.ledway.btprinter.fragments.MyIDFragment;
import com.ledway.btprinter.fragments.PagerFragment;
import com.ledway.btprinter.fragments.ShareAppFragment;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by togb on 2016/7/30.
 */
public class BusinessCardActivity extends AppCompatActivity {
  private EditText editText;
  private ViewPager mViewPager;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_bussiness_card);


    mViewPager = (ViewPager) findViewById(R.id.viewpager);
    mViewPager.setAdapter(new MyProfileViewPagerAdapter(getSupportFragmentManager(), new PagerFragment[]{new BusinessCardFragment(), new MyIDFragment(), new ShareAppFragment()}));
    // END_INCLUDE (setup_viewpager)

    // BEGIN_INCLUDE (setup_slidingtablayout)
    // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
    // it's PagerAdapter set.
    SlidingTabLayout mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
    mSlidingTabLayout.setViewPager(mViewPager);



  }

  @Override protected void onDestroy() {
    super.onDestroy();
  }

}
