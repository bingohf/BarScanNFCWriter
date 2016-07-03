package com.ledway.btprinter.models;

import android.text.TextUtils;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.ledway.btprinter.MApp;
import com.ledway.framework.RemoteDB;
import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

/**
 * Created by togb on 2016/5/29.
 */

@Table(name = "sample_master") public class SampleMaster extends Model implements Serializable {
  private List<Prod> prods;

  @Column(name = "guid", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
  public String guid;

  @Column(name = "create_date") public Date create_date;

  @Column(name = "update_date") public Date update_date;

  @Column(name = "mac_address") public String mac_address;

  @Column(name = "line") public String line;
  @Column(name = "reader") public String reader;
  @Column(name = "qrcode") public String qrcode;
  @Column(name = "image1") private byte[] image1 = new byte[] {};
  @Column(name = "image2") private byte[] image2 = new byte[] {};
  @Column(name = "desc") private String desc;
  @Column(name = "isDirty") private boolean isDirty = true;

  private boolean mIsChanged = false;
  protected boolean isLoadedAll = false;
  public boolean isChanged(){
    return mIsChanged;
  }
  public void reset(){
    mIsChanged = false;
    isLoadedAll = false;
  }


  public SampleMaster() {
    super();
  }

  public byte[] getImage1() {
    return image1;
  }

  public void setImage1(byte[] image1) {
    mIsChanged = true;
    isDirty = true;
    this.image1 = image1;
  }

  public byte[] getImage2() {
    return image2;
  }

  public void setImage2(byte[] image2) {
    mIsChanged = true;
    isDirty = true;
    this.image2 = image2;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {

    isDirty = true;
    mIsChanged = true;
    this.desc = desc;
  }

  public List<Prod> items() {
    return getMany(Prod.class, "cust_record");
  }

  public void allSave() {
    if (!isHasData()) {
      return;
    }
    if (TextUtils.isEmpty(guid)) {
      guid = MApp.getApplication().getSystemInfo().getDeviceId() + "_" + System.currentTimeMillis();
    }
    if (create_date == null) {
      create_date = new Date();
    }

    mac_address = MApp.getApplication().getSystemInfo().getDeviceId();
    update_date = new Date();
    qrcode = "http://vip.ledway.com.tw/i/s.aspx?series=" + guid;
    save();
    int i = 0;
    for (Prod prod : prods) {
      prod.sampleMaster = this;
      prod.ext = ++i;
      prod.save();
    }
  }

  public void queryDetail() {
    reset();
    if (getId() != null) {
      List<SampleMaster> record = new Select().from(this.getClass()).where("id = ?", getId()).execute();
      SampleMaster temp = record.get(0);
      create_date = temp.create_date;
      update_date = temp.update_date;
      desc = temp.desc;
      image1 = temp.image1;
      image2 = temp.image2;
      line = temp.line;
      reader = temp.reader;
      qrcode = temp.qrcode;
      isDirty = temp.isDirty;
      guid = temp.guid;
      mac_address = temp.mac_address;
    }
    if (getId() != null) {
      prods = items();
    } else {
      prods = new ArrayList<>();
    }
    isLoadedAll = true;

  }

  public Iterator<Prod> prodIterator(){

    return prods.iterator();
  }

  public int  addProd(String text){
    isDirty = true;
    mIsChanged = true;
    Prod prod = new Prod(text);
    prods.add(prod);
     prod.ext = prods.size();
    return prod.ext;
  }

  public Observable<SampleMaster> remoteSave() {
    if (!isLoadedAll){
      queryDetail();
    }
    String connectionString =
        "jdbc:jtds:sqlserver://vip.ledway.com.tw:1433;DatabaseName=iSamplePub;charset=UTF8";
    final RemoteDB remoteDB = new RemoteDB(connectionString);
    allSave();
    Observable<SampleMaster> observableMaster =
        remoteDB.executeProcedure("{call sp_UpSample(?,?,?,?,?,?,?,?)}",
            new int[] { Types.INTEGER, Types.VARCHAR }, 1, 1, guid, mac_address, desc, image1)
            .flatMap(new Func1<ArrayList<Object>, Observable<SampleMaster>>() {
              @Override public Observable<SampleMaster> call(ArrayList<Object> objects) {
                int returnCode = (Integer) objects.get(0);
                String returnMessage = (String) objects.get(1);
                if (returnCode == 1) {
                  qrcode = "http://vip.ledway.com.tw/i/s.aspx?series=" + guid;
                  save();
                  return Observable.just(SampleMaster.this);
                } else {
                  return Observable.error(new Exception(returnMessage));
                }
              }
            });

    Observable<SampleMaster> observableDetail =
        Observable.from(prods).flatMap(new Func1<Prod, Observable<SampleMaster>>() {
          @Override public Observable<SampleMaster> call(final Prod prod) {
            return remoteDB.executeProcedure("{call sp_UpSampleDetail(?,?,?,?,?,?,?,?,?,?)}",
                new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR }, 1, 1, mac_address, guid,
                prod.barcode, prod.ext, 1)
                .flatMap(new Func1<ArrayList<Object>, Observable<SampleMaster>>() {
                  @Override public Observable<SampleMaster> call(ArrayList<Object> objects) {
                    int returnCode = (Integer) objects.get(0);
                    String returnMessage = (String) objects.get(1);
                    String outProdNo = (String) objects.get(2);
                    if (returnCode == 1) {
                      prod.outProdNo = outProdNo;

                      if (!TextUtils.isEmpty(prod.outProdNo)){
                        TodoProd todoProd = new TodoProd();
                        todoProd.prodNo = prod.barcode;
                        todoProd.created_time = new Date();
                        todoProd.save();
                      }
                      prod.save();
                      return Observable.just(SampleMaster.this);
                    } else {
                      return Observable.error(new Exception(returnMessage));
                    }
                  }
                });
          }
        });
    return observableMaster.concatWith(observableDetail).doOnCompleted(new Action0() {
      @Override public void call() {
        isDirty = false;
        save();
      }
    });
  }

  public boolean isUploaded() {
    return !isDirty;
  }

  public boolean isHasData() {
    return !TextUtils.isEmpty(desc) || (image1 != null && image1.length > 0) || (image2 != null
        && image2.length > 0) || (prods != null && prods.size() > 0);
  }


}
