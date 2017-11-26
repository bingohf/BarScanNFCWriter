package com.ledway.btprinter.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import java.io.Serializable;
import java.util.Date;

@Table(name = "ReceivedSample")
public class ReceivedSample extends Model implements Serializable {
  @Column(name = "hold_id") public String holdId;
  @Column(name = "datetime") public Date datetime;
  @Column(name = "title") public String title;
  @Column(name = "icon_path") public String iconPath;
}
