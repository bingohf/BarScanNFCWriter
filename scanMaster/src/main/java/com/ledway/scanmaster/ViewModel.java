package com.ledway.scanmaster;

import android.arch.lifecycle.MutableLiveData;
import android.support.v4.view.ViewPager;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by togb on 2018/1/7.
 */

@Singleton
public class ViewModel{
  public final MutableLiveData<Long> settingChanged = new MutableLiveData<>();
  @Inject ViewModel(){
    settingChanged.setValue(System.currentTimeMillis());
  }
}
