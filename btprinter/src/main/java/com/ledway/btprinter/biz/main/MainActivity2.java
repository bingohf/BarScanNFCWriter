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
    Class< Fragment>[] fragmentCls = new Class[]{SampleListFragment.class,ReceiveSampleListFragment.class, ProductListFragment.class};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        ButterKnife.bind(this);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override public Fragment getItem(int position) {
              try {
                return fragmentCls[position].newInstance();
              } catch (InstantiationException e) {
                e.printStackTrace();
              } catch (IllegalAccessException e) {
                e.printStackTrace();
              }
              return null;
            }

            @Override public int getCount() {
                return fragmentCls.length;
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
