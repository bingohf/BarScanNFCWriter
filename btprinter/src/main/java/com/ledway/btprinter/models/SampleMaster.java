package com.ledway.btprinter.models;

import android.os.Parcelable;
import android.text.TextUtils;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.ledway.btprinter.MApp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Text;

/**
 * Created by togb on 2016/5/29.
 */

@Table(name = "sample_master")
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
  public byte[] image1;

  @Column(name = "image2")
  public byte[] image2;
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

  public void insertOrUpdate() {
    if (TextUtils.isEmpty(guid)){
      guid  = MApp.getApplication().getSystemInfo().getMacAddress() + "_" + System.currentTimeMillis();
    }
  }

}
