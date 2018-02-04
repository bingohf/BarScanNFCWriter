package com.ledway.btprinter.models;

import android.text.TextUtils;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.btprinter.network.model.RestSpResponse;
import com.ledway.btprinter.network.model.SpReturn;
import com.ledway.btprinter.network.model.Sp_UpProduct_Request;
import com.ledway.scanmaster.utils.IOUtil;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by togb on 2016/7/3.
 */
@Table(name = "todo_prod") public class TodoProd extends Model {
  @Column(name = "prodno", unique = true, onUniqueConflict = Column.ConflictAction.FAIL)
  public String prodNo;
  @Column(name = "image1") public String image1;
  @Column(name = "image2") public String image2;
  @Column(name = "uploaded_time") public Date uploaded_time;
  @Column(name = "update_time") public Date update_time;
  @Column(name = "create_time") public Date create_time = new Date();
  @Column(name = "spec_desc") public String spec_desc;

  public int todayCount;

  public int totalCount;

  public static Observable<TodoProd> getTodoProd(final String prodNo) {
    return Observable.create(new Observable.OnSubscribe<TodoProd>() {
      @Override public void call(final Subscriber<? super TodoProd> subscriber) {
        List<TodoProd> records =
            new Select(new String[] { "id", "prodno", "spec_desc" }).from(TodoProd.class)
                .where("prodno=?", prodNo)
                .execute();
        if (records.size() == 1) {
          subscriber.onNext(records.get(0));
          subscriber.onCompleted();
        } else {
          TodoProd todoProd = new TodoProd();
          todoProd.prodNo = prodNo;
          todoProd.create_time = new Date();
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

  public Observable<RestSpResponse<SpReturn>> remoteSave2() {
    String mac_address = MApp.getApplication().getSystemInfo().getDeviceId();
    byte[] image1Buffer = new byte[] {};
    byte[] image2Buffer = new byte[] {};
    if (!TextUtils.isEmpty(image1)) {
      try {
        image1Buffer = IOUtil.readFile(image1);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    if (!TextUtils.isEmpty(image2)) {
      try {
        image2Buffer = IOUtil.readFile(image2);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    Sp_UpProduct_Request request = new Sp_UpProduct_Request();
    request.line = 1;
    request.reader = 1;
    request.empno = mac_address;
    request.prodno = prodNo;
    request.specdesc = spec_desc;
    request.graphic = android.util.Base64.encodeToString(image1Buffer, android.util.Base64.DEFAULT);
    request.graphic2 =
        android.util.Base64.encodeToString(image2Buffer, android.util.Base64.DEFAULT);
    return MyProjectApi.getInstance().getDbService().sp_UpProduct(request);
  }

  public void queryAllField() {
    if (getId() != null) {
      List<TodoProd> record = new Select().from(this.getClass()).where("id = ?", getId()).execute();
      TodoProd temp = record.get(0);
      image1 = temp.image1;
      image2 = temp.image2;
      create_time = temp.create_time;
      uploaded_time = temp.uploaded_time;
      spec_desc = temp.spec_desc;
    }
  }
}
