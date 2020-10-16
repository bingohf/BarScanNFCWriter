package com.ledway.btprinter.biz.main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;

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