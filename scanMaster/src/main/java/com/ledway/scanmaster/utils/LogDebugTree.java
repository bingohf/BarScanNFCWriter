package com.ledway.scanmaster.utils;

import android.content.Context;
import android.os.Environment;
import com.ledway.scanmaster.BuildConfig;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import timber.log.Timber;

/**
 * Created by togb on 2017/3/4.
 */

public class LogDebugTree extends Timber.DebugTree {
  private BufferedWriter bufferedWriter;
  private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd HHmmss.S");
  public LogDebugTree(Context context){
    super();
    buildFile();
  }

  private void buildFile() {
    String time = simpleDateFormat.format(new Date());
    String logFileName = Environment.getExternalStorageDirectory().getAbsolutePath() +"/ledwayLog/" + BuildConfig.APPLICATION_ID +"/"+ time +".txt";
    File logFile  = new File(logFileName);
    logFile.getParentFile().mkdirs();
    try {
      bufferedWriter = new BufferedWriter(new FileWriter(logFile, true), 50);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override protected void log(int priority, String tag, String message, Throwable t) {
    super.log(priority, tag, message, t);
    if(bufferedWriter != null) {
      try {
        bufferedWriter.write(String.format("%s %d/%s:%s", simpleDateFormat.format(System.currentTimeMillis()),
            priority, tag, message));
        bufferedWriter.newLine();
        bufferedWriter.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
