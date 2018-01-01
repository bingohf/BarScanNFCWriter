package com.ledway.scanmaster;

import android.content.Context;
import com.ledway.scanmaster.data.SPModel;
import com.ledway.scanmaster.data.Settings;
import com.ledway.scanmaster.domain.TimeIDGenerator;
import com.ledway.scanmaster.interfaces.IDGenerator;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Created by togb on 2017/2/18.
 */
@Singleton @Module public class AppModule {
  private Context mContext;

  public AppModule(Context context) {
    mContext = context;
  }

  @Provides @ApplicationContext Context provideContext() {
    return mContext;
  }

  @Provides @Singleton IDGenerator provideIDGenerator() {
    return new TimeIDGenerator(mContext);
  }

  @Provides @Singleton Settings provideSettings(SPModel spModel) {
    return new Settings(spModel);
  }
}
