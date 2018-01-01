package com.ledway.scanmaster.data;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

/**
 * Created by togb on 2017/2/18.
 */
@Singleton
@Module
public class DataModule {
  @Singleton @Provides ConnectionPool provideConnectionPool(){
    return new ConnectionPool();
  }

}
