package com.ledway.btprinter.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import java.util.Date;

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
}
