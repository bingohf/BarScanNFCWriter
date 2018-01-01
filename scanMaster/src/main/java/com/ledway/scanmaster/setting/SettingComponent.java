package com.ledway.scanmaster.setting;

import com.ledway.scanmaster.AppComponent;
import com.ledway.scanmaster.utils.ActivityScope;
import dagger.Component;

/**
 * Created by togb on 2017/3/5.
 */
@ActivityScope @Component(dependencies = { AppComponent.class }, modules = { SettingModule.class })
public interface SettingComponent {
  void inject(AppPreferences appPreferences);
}
