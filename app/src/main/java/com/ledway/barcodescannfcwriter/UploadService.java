package com.ledway.barcodescannfcwriter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.ledway.barcodescannfcwriter.models.Record;

import com.ledway.framework.RemoteDB;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import rx.Notification;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by togb on 2016/4/23.
 */
public class UploadService {
    private RemoteDB remoteDB;
    private Context context;
    private Settings settings;
    private Statement statement;
    private static final DateFormat dateFormat = new SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss");
    public UploadService(Context context){
        this.context = context;
        settings = MApp.getInstance().getSettings();
        reset();
    }

    public Observable<Record> uploadRecord(final Record record){
        String sql = "insert into dbo.RFID1(line,reader,readings,wK_date,rfid_series) values(?,?,?,?,?)";

        return remoteDB.execute(sql, record.line, record.reader, record.readings, record.wk_date, record.rfidSeries)
            .map(new Func1<Boolean, Record>() {
                @Override public Record call(Boolean aBoolean) {
                    record.uploaded_datetime = new Date();
                    record.save();
                    return record;
                }
            }).doOnError(new Action1<Throwable>() {
                @Override public void call(Throwable throwable) {
                    reset();
                }
            }).retry(2);

    }

    private synchronized void prepareStatement() throws Exception {
        boolean isClosed = true;
        try {
            if(statement != null) {
                isClosed = statement.isClosed();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (statement == null || isClosed){
            Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
            String connectionString = "jdbc:jtds:sqlserver://%s;DatabaseName=WINUPRFID;charset=UTF8";
            connectionString = String.format(connectionString, settings.getServer());
            Connection conn = DriverManager.getConnection(connectionString,
                    "sa", "ledway");
            statement = conn.createStatement();
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void reset() {
        if (remoteDB != null){
            remoteDB.reset();
        }
        String connectionString = "jdbc:jtds:sqlserver://%s;DatabaseName=WINUPRFID;charset=UTF8";
        connectionString = String.format(connectionString, settings.getServer());
        remoteDB = new RemoteDB(connectionString);
    }
}
