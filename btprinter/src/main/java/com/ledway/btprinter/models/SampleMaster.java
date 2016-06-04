package com.ledway.btprinter.models;

import android.os.Parcelable;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by togb on 2016/5/29.
 */

@Table(name = "cust_record")
public class SampleMaster extends Model implements Serializable {
  public List<Prod> prods;


  @Column(name = "guid", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
  public String guid;

  @Column(name = "line")
  public String line;

  @Column(name = "reader")
  public String reader;

  @Column(name = "desc")
  public String desc;

  @Column(name = "image1")
  public String image1;

  @Column(name = "image2")
  public String image2;
  public List<Prod> items() {
    return getMany(Prod.class, "cust_record");
  }

  public SampleMaster(){
    super();
    if (getId() != null){
      prods = items();
    }else {
      prods = new ArrayList<>();
    }

  }

}
