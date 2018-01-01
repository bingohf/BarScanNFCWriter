package com.ledway.scanmaster.setting;

import com.ledway.scanmaster.domain.LedwayPaswordVerify;
import com.ledway.scanmaster.interfaces.PasswordVerify;
import com.ledway.scanmaster.utils.ActivityScope;
import dagger.Module;
import dagger.Provides;

/**
 * Created by togb on 2017/3/5.
 */
@ActivityScope
@Module
public class SettingModule {

  @Provides PasswordVerify providePasswordVerfiy(){
    return new LedwayPaswordVerify();
  }
}
