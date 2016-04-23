package com.ledway.barcodescannfcwriter.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.Date;

/**
 * Created by togb on 2016/3/27.
 */

@Table(name = "record")
public class Record extends Model{
    @Column(name = "readings", index = true)
    public String readings;

    @Column(name = "wk_date", index = true)
    public Date wk_date;

    @Column(name = "line")
    public String line;

    @Column(name = "reader")
    public String reader;

    @Column(name = "lwGuid")
    public String lwGuid;

    @Column(name = "uploaded_datetime")
    public Date uploaded_datetime;

    @Column(name = "logMessage")
    public String logMessage;

    @Column(name ="RFID_Series")
    public String rfidSeries;
    public Record() {
        super();
    }
}
