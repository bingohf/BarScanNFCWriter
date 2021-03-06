package com.ledway.btprinter.biz.sample;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import com.ledway.btprinter.R;

public class SampleFragmentPagerAdapter extends FragmentPagerAdapter {
    int[] titles = new int[]{ R.string.customer, R.string.wish_list};
    private Fragment[] fragments = new Fragment[]{new SampleMainFragment(), new SampleProductListFragment()};
    private Context context;

    public SampleFragmentPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
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
        return context.getString(titles[position]);
    }
}