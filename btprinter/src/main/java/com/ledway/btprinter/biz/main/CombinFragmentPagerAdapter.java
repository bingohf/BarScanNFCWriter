package com.ledway.btprinter.biz.main;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.biz.sample.SampleMainFragment;
import com.ledway.btprinter.biz.sample.SampleProductListFragment;

public class CombinFragmentPagerAdapter extends FragmentPagerAdapter {
    int[] titles = new int[]{ R.string.my_list, R.string.received};
    private Fragment[] fragments = new Fragment[]{new SampleListFragment(), new ReceiveSampleListFragment()};

    public CombinFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments[position];
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return MApp.getApplication().getString(titles[position]);
    }
}