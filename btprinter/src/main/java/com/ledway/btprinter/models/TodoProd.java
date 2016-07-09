package com.ledway.btprinter.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.ledway.btprinter.MApp;
import com.ledway.framework.RemoteDB;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by togb on 2016/7/3.
 */
@Table(name = "todo_prod") public class TodoProd extends Model {
  @Column(name = "prodno", unique = true, onUniqueConflict = Column.ConflictAction.FAIL)
  public String prodNo;
  @Column(name = "image1") public byte[] image1 = new byte[] {};
  @Column(name = "image2") public byte[] image2 = new byte[] {};
  @Column(name = "uploaded_time") public Date uploaded_time;
  @Column(name = "created_time") public Date created_time;
  @Column(name = "spec_desc") public String spec_desc;

  public Observable<ArrayList<Object>> remoteSave(){
    String mac_address = MApp.getApplication().getSystemInfo().getDeviceId();
    String connectionString =
        "jdbc:jtds:sqlserver://vip.ledway.com.tw:1433;DatabaseName=iSamplePub;charset=UTF8";
    final RemoteDB remoteDB = new RemoteDB(connectionString);
    return remoteDB.executeProcedure("{call sp_UpProduct(?,?,?,?,?,?,?,?,?)}",
        new int[] { Types.INTEGER, Types.VARCHAR }, 1, 1, mac_address, prodNo,spec_desc,
        image1, image2);
  }

  public Observable<Boolean> sync(){
    String connectionString =
        "jdbc:jtds:sqlserver://vip.ledway.com.tw:1433;DatabaseName=iSamplePub;charset=UTF8";
    final RemoteDB remoteDB = new RemoteDB(connectionString);
    return remoteDB.executeQuery("select specdesc from product where prodno=?", prodNo)
        .map(new Func1<ResultSet, Boolean>() {
          @Override public Boolean call(ResultSet resultSet) {
            try {
              while(resultSet.next()){
                spec_desc = resultSet.getString("specdesc");
                return true;
              }
            } catch (SQLException e) {
              e.printStackTrace();
              return false;
            }
            return false;
          }
        });
  }

  public void queryAllField(){
    if (getId() != null) {
      List<TodoProd> record = new Select().from(this.getClass()).where("id = ?", getId()).execute();
      TodoProd temp = record.get(0);
      image1 = temp.image1;
      image2 = temp.image2;
      created_time = temp.created_time;
      uploaded_time = temp.uploaded_time;
      spec_desc = temp.spec_desc;
    }
  }

}
