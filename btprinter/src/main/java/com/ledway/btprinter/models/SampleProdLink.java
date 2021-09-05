package com.ledway.btprinter.models;

import android.text.TextUtils;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import java.util.Date;



public class SampleProdLink {

  public String prod_id;

  public String prodNo;


  public Date create_time;
  public Date create_date;

  public int ext;

  public String image1;

  public String spec_desc = "";

  public Date uploaded_time;
  public Date update_time;
  public Date update_date;
  public int count = 1;
  public String memo = "";

  public String toSpec(){
    if(TextUtils.isEmpty(spec_desc)){
      return "";
    }
    return spec_desc;
  }

}
