package com.ledway.scanmaster.nfc;

import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;

/**
 * Created by togb on 2016/10/30.
 */

public class GNfcLoader {
  public static final  String[][] TechList =new String[][] {
    new String[] { MifareClassic.class.getName() },
        new String[] { Ndef.class.getName() } };// 允许扫描的标签类型

  public static GNfc load(Tag tag){
    for(String tech : tag.getTechList()){
      if (tech.equals(MifareClassic.class.getName())){
        return new GMifareNfc(tag);
      }else  if(tech.equals(Ndef.class.getName() )){
        return new GNdef(tag);
      }
    }
    return new GMifareNfc(tag);
  }
}
