package com.ledway.btprinter.biz.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import java.util.Date;
import java.util.List;
import org.w3c.dom.Text;

public class SampleActivity extends AppCompatActivity {
  @BindView(R.id.sliding_tabs) TabLayout mTabLayout;
  @BindView(R.id.viewpager) ViewPager mViewPager;
  SampleMaster mSampleMaster;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String guid = getIntent().getStringExtra("guid");
    loadSampleMaster(guid);
    setContentView(R.layout.activity_sample);

    ButterKnife.bind(this);

    String sampleLabel = "NA";
    if(!TextUtils.isEmpty(mSampleMaster.desc)){
      sampleLabel = mSampleMaster.desc;
    }
    getSupportActionBar().setTitle(getString(R.string.formater_sample_title, sampleLabel));
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

  @Override protected void onDestroy() {
    super.onDestroy();
  }

  @Override public void onBackPressed() {
    if(mSampleMaster.update_date != null) {
      for(SampleProdLink item :mSampleMaster.sampleProdLinks){
        item.save();
      }
      mSampleMaster.save();
    }
    super.onBackPressed();

  }

  private void loadSampleMaster(String guid) {
    mSampleMaster = new SampleMaster();
    mSampleMaster.guid =
        MApp.getApplication().getSystemInfo().getDeviceId() + "_" + System.currentTimeMillis();

    if (guid != null) {
      List<Model> list = new Select().from(SampleMaster.class).where("guid =?", guid).execute();
      if (!list.isEmpty()) {
        mSampleMaster = (SampleMaster) list.get(0);
        mSampleMaster.fetchLink();
      }
    }
    mSampleMaster.dataFrom = MApp.getApplication().getSystemInfo().getBusinessCard();
  }
}
