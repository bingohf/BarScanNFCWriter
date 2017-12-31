package com.ledway.btprinter.models;

import android.text.TextUtils;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import java.util.Date;

/**
 * Created by togb on 2016/7/10.
 */
@Table(name = "SampleProdLink")
public class SampleProdLink extends Model{

  @Column(name = "link_id", unique = true, onUniqueConflict = Column.ConflictAction.FAIL)
  public String link_id;

  @Column(name = "sample_id")
  public String sample_id;

  @Column(name = "prod_id")
  public String prod_id;

  @Column(name = "create_date")
  public Date create_date;

  @Column(name = "ext")
  public int ext;


  public String spec_desc = "";

  public String toSpec(){
    if(TextUtils.isEmpty(spec_desc)){
      return "";
    }
    return spec_desc;
  }

}
