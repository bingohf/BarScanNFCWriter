package com.ledway.btprinter.models;

import android.text.TextUtils;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.utils.IOUtil;
import com.ledway.framework.RemoteDB;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Created by togb on 2016/7/3.
 */
@Table(name = "todo_prod") public class TodoProd extends Model {
  @Column(name = "prodno", unique = true, onUniqueConflict = Column.ConflictAction.FAIL)
  public String prodNo;
  @Column(name = "image1") public String image1;
  @Column(name = "image2") public String image2;
  @Column(name = "uploaded_time") public Date uploaded_time;
  @Column(name = "created_time") public Date created_time;
  @Column(name = "spec_desc") public String spec_desc;

  public Observable<ArrayList<Object>> remoteSave(){
    String mac_address = MApp.getApplication().getSystemInfo().getDeviceId();
    String connectionString =
        "jdbc:jtds:sqlserver://vip.ledway.com.tw:1433;DatabaseName=iSamplePub;charset=UTF8";
    final RemoteDB remoteDB = new RemoteDB(connectionString);
    byte[] image1Buffer = new byte[]{};
    byte[] image2Buffer = new byte[]{};
    if(!TextUtils.isEmpty(image1)){
      try {
        image1Buffer = IOUtil.readFile(image1);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if(! TextUtils.isEmpty(image2)){
      try {
        image2Buffer = IOUtil.readFile(image2);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return remoteDB.executeProcedure("{call sp_UpProduct(?,?,?,?,?,?,?,?,?)}",
        new int[] { Types.INTEGER, Types.VARCHAR }, 1, 1, mac_address, prodNo,spec_desc,
        image1Buffer, image2Buffer);
  }

  public Observable<TodoProd> sync(){
    String connectionString =
        "jdbc:jtds:sqlserver://vip.ledway.com.tw:1433;DatabaseName=iSamplePub;charset=UTF8";
    final RemoteDB remoteDB = new RemoteDB(connectionString);
    return remoteDB.executeQuery("select specdesc from product where prodno=? and empNo =?", prodNo, MApp.getApplication().getSystemInfo().getDeviceId())
        .map(new Func1<ResultSet, TodoProd>() {
          @Override public TodoProd call(ResultSet resultSet) {
            try {
              while(resultSet.next()){
                spec_desc = resultSet.getString("specdesc");
                return TodoProd.this;
              }
            } catch (SQLException e) {
              e.printStackTrace();
              return TodoProd.this;
            }
            return TodoProd.this;
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
  public static Observable<TodoProd> getTodoProd(final String prodNo){
    return Observable.create(new Observable.OnSubscribe<TodoProd>() {
      @Override public void call(final Subscriber<? super TodoProd> subscriber) {
        List<TodoProd> records = new Select(new String[]{"id","prodno","spec_desc"}).from(TodoProd.class).where("prodno=?", prodNo).execute();
        if (records.size() ==1){
          subscriber.onNext(records.get(0));
          subscriber.onCompleted();
        }else{
          TodoProd todoProd = new TodoProd();
          todoProd.prodNo = prodNo;
          todoProd.created_time = new Date();
          todoProd.save();
          subscriber.onNext(todoProd);
          subscriber.onCompleted();
         /* todoProd.sync().subscribe(new Subscriber<TodoProd>() {
            @Override public void onCompleted() {
              subscriber.onCompleted();
            }

            @Override public void onError(Throwable e) {
              subscriber.onError(e);
            }

            @Override public void onNext(TodoProd todoProd) {
              subscriber.onNext(todoProd);
            }
          });*/

        }
      }
    });
  }


}
