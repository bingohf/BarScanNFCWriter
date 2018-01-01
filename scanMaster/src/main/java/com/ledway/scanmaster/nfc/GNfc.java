package com.ledway.scanmaster.nfc;

import android.nfc.Tag;
import java.io.IOException;

/**
 * Created by togb on 2016/10/30.
 */

public abstract class GNfc {
  public GNfc(Tag tag) {
  }
  public static final int PROPERTY_READABLE = 0x01;
  public abstract String read() throws IOException;

  public abstract void write(String text) throws IOException;

  public abstract void connect() throws IOException;

  public abstract int getProperty();

  public abstract void close() throws IOException;
}
