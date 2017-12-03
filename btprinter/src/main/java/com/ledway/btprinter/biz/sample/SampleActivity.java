package com.ledway.btprinter.biz.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.ledway.btprinter.R;


public class SampleActivity extends AppCompatActivity {
  @BindView(R.id.sliding_tabs) TabLayout mTabLayout;
  @BindView(R.id.viewpager) ViewPager mViewPager;
  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sample);
    ButterKnife.bind(this);
    mViewPager.setAdapter(new SampleFragmentPagerAdapter(getSupportFragmentManager(),
        this));
    mTabLayout.setupWithViewPager(mViewPager);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override public void onPageSelected(int position) {
        invalidateOptionsMenu();
      }

      @Override public void onPageScrollStateChanged(int state) {

      }
    });
  }
}
