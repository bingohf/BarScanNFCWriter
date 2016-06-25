package com.ledway.btprinter.models;

import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.Toast;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.ledway.btprinter.MApp;
import com.ledway.framework.RemoteDB;
import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.w3c.dom.Text;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/5/29.
 */

@Table(name = "sample_master")
public class SampleMaster extends Model implements Serializable {
  public List<Prod> prods;

  @Column(name = "guid", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
  public String guid;

  @Column(name = "create_date")
  public Date create_date;

  @Column(name = "update_date")
  public Date update_date;

  @Column(name = "mac_address")
  public String mac_address;

  @Column(name = "line")
  public String line;

  @Column(name = "reader")
  public String reader;

  @Column(name = "desc")
  public String desc;

  @Column(name = "image1")
  public byte[] image1 = new byte[]{};

  @Column(name = "image2")
  public byte[] image2 =new byte[]{};

  @Column(name = "qrcode")
  public String qrcode;

  @Column(name = "isDirty")
  private boolean isDirty = true;

  public List<Prod> items() {
    return getMany(Prod.class, "cust_record");
  }

  public SampleMaster(){
    super();
  }

  public void allSave() {
    if (!isHasData()){
      return;
    }
    if (TextUtils.isEmpty(guid)){
      guid  = MApp.getApplication().getSystemInfo().getDeviceId() + "_" + System.currentTimeMillis();
    }
    if (create_date == null){
      create_date = new Date();
    }

    mac_address = MApp.getApplication().getSystemInfo().getDeviceId();
    update_date = new Date();
    qrcode =
        "http://vip.ledway.com.tw/i/s.aspx?series=" + guid;
    save();
    int i = 0;
    for(Prod prod :prods){
      prod.sampleMaster = this;
      prod.ext = ++i;
      prod.save();
    }
  }

  public void queryDetail() {
    if (getId() != null){
      prods = items();
    }else {
      prods = new ArrayList<>();
    }
  }

  public Observable<SampleMaster> remoteSave() {

    String connectionString =
        "jdbc:jtds:sqlserver://vip.ledway.com.tw:1433;DatabaseName=iSamplePub;charset=UTF8";
    final RemoteDB  remoteDB = new RemoteDB(connectionString);
    allSave();
    Observable<SampleMaster> observableMaster =
        remoteDB.executeProcedure("{call sp_UpSample(?,?,?,?,?,?,?,?)}",
            new int[] { Types.INTEGER, Types.VARCHAR }, 1, 1, guid, mac_address, desc, image1)
            .flatMap(new Func1<ArrayList<Object>, Observable<SampleMaster>>() {
              @Override public Observable<SampleMaster> call(ArrayList<Object> objects) {
                int returnCode = (Integer) objects.get(0);
                String returnMessage = (String) objects.get(1);
                if (returnCode == 1) {
                  qrcode =
                      "http://vip.ledway.com.tw/i/s.aspx?series=" + guid;
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


  public boolean isHasData(){
    return !TextUtils.isEmpty(desc) || (image1 != null && image1.length >0)
        || (image2 != null && image2.length >0)
        || (prods!= null && prods.size() >0);
  }

}