package com.ledway.scanmaster.data;

import javax.inject.Inject;

/**
 * Created by togb on 2017/2/18.
 */

public class Settings {
  private final SPModel spModel;
  public String server;
  public String db;
  public String line;
  public String reader;

  public Settings(SPModel spModel){
    this.spModel = spModel;
    restore();
  }
  void restore(){
    restore(spModel.loadSetting());
  }

  private void restore(SettingSnap settingSnap){
    server = settingSnap.server;
    db = settingSnap.db;
    line = settingSnap.line;
    reader = settingSnap.reader;
  }

  public String getServer() {
    return server;
  }

  public void setServer(String server) {
    this.server = server;
    save();
  }

  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
    save();
  }

  public String getLine() {
    return line;
  }

  public void setLine(String line) {
    this.line = line;
    save();
  }

  public String getReader() {
    return reader;
  }

  public void setReader(String reader) {
    this.reader = reader;
    save();
  }

  private SettingSnap toSnap(){
    SettingSnap settingSnap = new SettingSnap();
    settingSnap.server = server;
    settingSnap.db = db;
    settingSnap.line = line;
    settingSnap.reader = reader;
    return settingSnap;
  }

  private void save(){
    spModel.save(toSnap());
  }
}
