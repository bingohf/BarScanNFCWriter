package com.ledway.btprinter.biz.main;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.ledway.btprinter.R;

public class CombinFramgment extends Fragment {
  @BindView(R.id.sliding_tabs) TabLayout mTabLayout;
  @BindView(R.id.viewpager) ViewPager mViewPager;
  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_view_pager, container,false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ButterKnife.bind(this, view);

    mViewPager.setAdapter(new CombinFragmentPagerAdapter(getChildFragmentManager()));
    mTabLayout.setupWithViewPager(mViewPager);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override public void onPageSelected(int position) {
     //   invalidateOptionsMenu();
      }

      @Override public void onPageScrollStateChanged(int state) {

      }
    });

  }
}
