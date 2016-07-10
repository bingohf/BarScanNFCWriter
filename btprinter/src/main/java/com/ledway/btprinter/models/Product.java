package com.ledway.btprinter.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import java.io.Serializable;

/**
 * Created by togb on 2016/5/29.
 */
@Table(name = "product")
public class Product extends Model implements Serializable {
  @Column(name = "bar_code")
  public String barcode;
  @Column(name = "cust_record")
  public SampleMaster sampleMaster;

  @Column(name = "ext")
  public int ext;

  @Column(name = "outProdNo")
  public String outProdNo;

  public Product(){

  }

  public Product(String barcode){
    this.barcode = barcode;
  }

}
