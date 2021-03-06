package com.ledway.scanmaster;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

 class FragmentPagerAdapter extends androidx.fragment.app.FragmentPagerAdapter {
    int[] titles = new int[]{ R.string.in, R.string.out, R.string.check};
    private Fragment[] fragments = new Fragment[]{new Fragment(), new Fragment()};

    public FragmentPagerAdapter(FragmentManager fm) {
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
        return MApp.getInstance().getString(titles[position]);
    }
}