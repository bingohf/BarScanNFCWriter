package com.ledway.scanmaster.nfc;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import java.io.IOException;

/**
 * Created by togb on 2016/10/30.
 */

public class GMifareNfc extends GNfc {

  private final static byte keyA[] = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
      (byte) 0xff, (byte) 0xff};
  private final MifareClassic nfc;
  private boolean authed;

  public GMifareNfc(Tag tag) {
    super(tag);
    this.nfc = MifareClassic.get(tag);
  }

  @Override public String read() throws IOException {
     if (authed){
       nfc.sectorToBlock(4);
       byte[] bytes = nfc.readBlock(4);
       String barcode = new String(bytes).trim();
       return barcode.trim();
     }
    return "";
  }

  @Override public void write(String text) throws IOException {
    byte[] d = text.getBytes();
    byte[] f = new byte[16];
    for (int j = 0; j < d.length; j++) {
      f[j] = d[j];
    }
    if (d.length < 16) {
      int j = 16 - d.length;
      int k = d.length;
      for (int j2 = 0; j2 < j; j2++) {
        f[k + j2] = (byte) 0x00;
      }
    }
    nfc.writeBlock(4, f);

  }

  @Override public void connect() throws IOException {
    nfc.connect();
    this.authed = nfc.authenticateSectorWithKeyA(
        1,
        keyA);
  }

  @Override public int getProperty() {
    if(authed){
      return PROPERTY_READABLE;
    }
    return 0;
  }

  @Override public void close() throws IOException {
    nfc.close();
  }
}
