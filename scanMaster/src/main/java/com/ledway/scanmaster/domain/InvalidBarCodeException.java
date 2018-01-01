package com.ledway.scanmaster.domain;

import com.ledway.scanmaster.MApp;
import com.ledway.scanmaster.R;

/**
 * Created by togb on 2017/2/26.
 */

public class InvalidBarCodeException extends Exception {
  public InvalidBarCodeException() {
    super(MApp.getInstance().getString(R.string.invalid_barcode));
  }
}
