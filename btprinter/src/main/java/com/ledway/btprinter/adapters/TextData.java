package com.ledway.btprinter.adapters;

import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by togb on 2016/6/4.
 */
public class TextData extends BaseData{
  public String text;
  public TextData(int type) {
    super(type);
  }
  public String getText(){
    return text;
  }
  public void setText(String value){
    text = value;
  }

  @Override public void printTo(OutputStream outputStream) throws IOException {
    String s =text;
    switch (type){
      case DataAdapter.DATA_TYPE_MEMO:{
        s = MApp.getApplication().getString(R.string.qr_card) + s;
        break;
      }
      case DataAdapter.DATA_TYPE_BARCODE:{
        s = MApp.getApplication().getString(R.string.prod)  + s;
        break;
      }
    }
    outputStream.write(new byte[]{0x1b,0x40});
    outputStream.write(s.getBytes("GBK"));
    outputStream.write(new byte[]{0x0a});
  }
}
