package com.ledway.btprinter.biz.main;

import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.ledway.btprinter.R;

public class MainActivity2 extends AppCompatActivity {
   @BindView(R.id.viewPager) ViewPager mViewPager;
   @BindView(R.id.bottomNavigation) BottomNavigationView mBottomNav;
    Fragment[] fragments = new Fragment[]{new SampleListFragment(), new ProductListFragment()};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override public Fragment getItem(int position) {
                return fragments[position];
            }

            @Override public int getCount() {
                return fragments.length;
            }
        });
      mBottomNav.setOnNavigationItemSelectedListener(item -> {
        mViewPager.setCurrentItem(item.getOrder());
        return true;
      });
      mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override public void onPageSelected(int position) {
          mBottomNav.setSelectedItemId(  mBottomNav.getMenu().getItem(position).getItemId());
        }

        @Override public void onPageScrollStateChanged(int state) {

        }
      });
    }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }
}
