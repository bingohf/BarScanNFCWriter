package com.ledway.btprinter.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.ledway.btprinter.MApp;
import com.ledway.framework.RemoteDB;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import rx.Observable;

/**
 * Created by togb on 2016/7/3.
 */
@Table(name = "todo_prod") public class TodoProd extends Model {
  @Column(name = "prodno", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
  public String prodNo;
  @Column(name = "image1") public byte[] image1 = new byte[] {};
  @Column(name = "image2") public byte[] image2 = new byte[] {};
  @Column(name = "uploaded_time") public Date uploaded_time;
  @Column(name = "created_time") public Date created_time;


  public Observable<ArrayList<Object>> remoteSave(){
    String mac_address = MApp.getApplication().getSystemInfo().getDeviceId();
    String connectionString =
        "jdbc:jtds:sqlserver://vip.ledway.com.tw:1433;DatabaseName=iSamplePub;charset=UTF8";
    final RemoteDB remoteDB = new RemoteDB(connectionString);
    return remoteDB.executeProcedure("{call sp_UpProduct(?,?,?,?,?,?,?,?)}",
        new int[] { Types.INTEGER, Types.VARCHAR }, 1, 1, mac_address, prodNo,
        image1, image2);
  }
}
