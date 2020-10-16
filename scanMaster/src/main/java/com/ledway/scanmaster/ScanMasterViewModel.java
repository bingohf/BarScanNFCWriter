package com.ledway.scanmaster;

import androidx.lifecycle.MutableLiveData;

/**
 * Created by togb on 2018/1/7.
 */

public class ScanMasterViewModel {
  public final MutableLiveData<String> reader = new MutableLiveData<>();

  private static ScanMasterViewModel instance;
  public static ScanMasterViewModel getInstance(){
    if(instance == null){
      instance = new ScanMasterViewModel();
    }
    return instance;
  }
}
