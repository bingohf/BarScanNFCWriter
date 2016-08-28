package com.ledway.btprinter.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.ledway.btprinter.fragments.PagerFragment;

/**
 * Created by togb on 2016/8/28.
 */
public class MyProfileViewPagerAdapter extends FragmentPagerAdapter {

  private final PagerFragment[] fragments;

  public MyProfileViewPagerAdapter(FragmentManager fm, PagerFragment[] fragments) {
    super(fm);
    this.fragments = fragments;
  }

  @Override public Fragment getItem(int position) {
    return fragments[position];
  }

  @Override public int getCount() {
    return fragments.length;
  }

  @Override public CharSequence getPageTitle(int position) {
    return fragments[position].getTitle();
  }
}
