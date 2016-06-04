package com.ledway.btprinter.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import java.io.Serializable;

/**
 * Created by togb on 2016/5/29.
 */
@Table(name = "prod")
public class Prod extends Model implements Serializable {
  @Column(name = "bar_code", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
  public String barcode;
  @Column(name = "cust_record", onUpdate = Column.ForeignKeyAction.CASCADE, onDelete = Column.ForeignKeyAction.CASCADE)
  public SampleMaster sampleMaster;


  public Prod(String barcode){
    this.barcode = barcode;
  }

}
