package com.ledway.btprinter;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.EditText;

import com.example.android.common.view.SlidingTabLayout;
import com.ledway.btprinter.adapters.MyProfileViewPagerAdapter;
import com.ledway.btprinter.fragments.BusinessCardFragment;
import com.ledway.btprinter.fragments.MyIDFragment;
import com.ledway.btprinter.fragments.PagerFragment;
import com.ledway.btprinter.fragments.ShareAppFragment;

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
