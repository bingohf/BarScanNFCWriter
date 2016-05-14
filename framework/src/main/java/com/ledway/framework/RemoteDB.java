package com.ledway.framework;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.util.Date;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

/**
 * Created by togb on 2016/5/14.
 */
public class RemoteDB {
  private final String mConnectiongString;
  private Connection connection;
  public RemoteDB(String connectionString){
    mConnectiongString = connectionString;
  }

  public Observable<Boolean> execute(final String sql, final Object... args){
    return Observable.create(new Observable.OnSubscribe<Boolean>() {
      @Override public void call(Subscriber<? super Boolean> subscriber) {

        try {
          initStatement();
          PreparedStatement preparedStatement = connection.prepareStatement(sql);
          for(int i =0;i < args.length ;++i){
            if (args[i] instanceof String){
              preparedStatement.setString(i + 1, (String)args[i]);
            } else if (args[i] instanceof Integer){
              preparedStatement.setInt(i + 1, (Integer) args[i]);
            } else if (args[i] instanceof Float){
              preparedStatement.setFloat(i + 1, (Float) args[i]);
            } else if (args[i] instanceof Date){
              Date date = (Date) args[i];
              preparedStatement.setTimestamp(i +1, new java.sql.Timestamp(date.getTime()));
            }else if (args[i] == null){
              preparedStatement.setNull(i +1, Types.VARCHAR);
            }else {
              throw new Exception("invalid sql & parameters");
            }
          }
          if (preparedStatement.executeUpdate() == 0){
            throw new Exception("execute fail " + sql);
          }
          subscriber.onNext(true);
          subscriber.onCompleted();
        } catch (Exception e) {
          e.printStackTrace();
          subscriber.onError(e);
        }
      }
    }).doOnError(new Action1<Throwable>() {
      @Override public void call(Throwable throwable) {
        if (connection != null){
          try {
            connection.close();
            connection = null;
          } catch (SQLException e) {
            e.printStackTrace();
          }

        }
      }
    }).retry(2);
  }


  public Observable<ResultSet> executeQuery(final String sql, final Object... args){
    return Observable.create(new Observable.OnSubscribe<ResultSet>() {
      @Override public void call(Subscriber<? super ResultSet> subscriber) {
        initStatement();
        try {
          CallableStatement callableStatement = connection.prepareCall(sql);
          for(int i =0;i < args.length ;++i){
            if (args[i] instanceof String){
              callableStatement.setString(i + 1, (String)args[i]);
            } else if (args[i] instanceof Integer){
              callableStatement.setInt(i + 1, (Integer) args[i]);
            } else if (args[i] instanceof Float){
              callableStatement.setFloat(i + 1, (Float) args[i]);
            } else if (args[i] instanceof Date){
              Date date = (Date) args[i];
              callableStatement.setDate(i + 1,new java.sql.Date(date.getTime()) );
            } else if (args[i] instanceof Time){
              callableStatement.setTime(i + 1, (Time) args[i]);
            }else{
              throw new Exception("invalid sql & parameters");
            }
          }
          ResultSet resultSet = callableStatement.executeQuery();
          subscriber.onNext(resultSet);
          subscriber.onCompleted();
        } catch (Exception e) {
          e.printStackTrace();
          subscriber.onError(e);
        }
      }
    }).doOnError(new Action1<Throwable>() {
      @Override public void call(Throwable throwable) {
        reset();
      }
    }).retry(2);
  }

  private synchronized void initStatement(){
    boolean isClosed = true;
    try {
      if(connection != null) {
        isClosed = connection.isClosed();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    if (connection == null || isClosed){
      try {
        Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
        String connectionString = "jdbc:jtds:sqlserver://%s;DatabaseName=WINUPRFID;charset=UTF8";
        connection = DriverManager.getConnection(mConnectiongString,
            "sa", "ledway");

      } catch (InstantiationException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }
  public synchronized void reset(){
    if (connection != null){
      try {
        connection.close();
        connection = null;
      } catch (SQLException e) {
        e.printStackTrace();
      }

    }
  }



}
