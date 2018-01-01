package com.ledway.scanmaster.data;

import android.text.TextUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by togb on 2017/2/18.
 */

public class DBCommand {
  private ConnectionPool connectionPool = new ConnectionPool();
  private String connectionString;

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

  public String execute(String sql, String... args)
      throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
    Timber.v("%s - %s", sql, TextUtils.join(",", args));
    Connection connection = connectionPool.getConnection(connectionString);
    CallableStatement csmt = connection.prepareCall(sql);
    int i = 0;
    for (; i < args.length; ++i) {
      csmt.setString(i + 1, args[i]);
    }
    csmt.registerOutParameter(i + 1, Types.VARCHAR);
    csmt.execute();
    return csmt.getString(i + 1);
  }

  public Observable<String> rxExecute(String sql, String... args) {
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override public void call(Subscriber<? super String> subscriber) {
        try {
          String msg = execute(sql, args);
          subscriber.onNext(msg);
          subscriber.onCompleted();
        } catch (Exception e) {
          Timber.e(e, e.getMessage());
          subscriber.onError(e);
        }
      }
    }).retry(2).subscribeOn(Schedulers.io());
  }
}
