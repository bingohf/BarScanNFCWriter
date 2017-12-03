package com.ledway.btprinter.biz.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.example.android.common.view.SlidingTabLayout;
import com.ledway.btprinter.R;
import com.ledway.btprinter.adapters.MyProfileViewPagerAdapter;
import com.ledway.btprinter.fragments.BusinessCardFragment;
import com.ledway.btprinter.fragments.MyIDFragment;
import com.ledway.btprinter.fragments.PagerFragment;
import com.ledway.btprinter.fragments.ShareAppFragment;

/**
 * Created by togb on 2017/12/3.
 */

public class MyAccountFragment extends Fragment {

  @BindView(R.id.viewpager) ViewPager mViewPager;
  @BindView(R.id.sliding_tabs) SlidingTabLayout mSlidingTabLayout;
  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.activity_bussiness_card,container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ButterKnife.bind(this, view);
    initView();
  }

  private void initView() {
    mViewPager.setAdapter(new MyProfileViewPagerAdapter(getChildFragmentManager(), new PagerFragment[]{new BusinessCardFragment(), new MyIDFragment(), new ShareAppFragment()}));
    // END_INCLUDE (setup_viewpager)

    // BEGIN_INCLUDE (setup_slidingtablayout)
    // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had

    mSlidingTabLayout.setViewPager(mViewPager);
  }
}
