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
    @Column(name = "barcode", index = true, unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public String barcode;

    @Column(name = "barcode", index = true)
    public Date datetime;
}
