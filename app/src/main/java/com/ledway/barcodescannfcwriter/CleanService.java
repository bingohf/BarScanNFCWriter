package com.ledway.barcodescannfcwriter;

import android.content.Context;
import com.activeandroid.query.Select;
import com.ledway.barcodescannfcwriter.models.Record;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by togb on 2016/4/30.
 */
public class CleanService {
  private static CleanService instance;
  private Context mContext;

  private CleanService(Context context) {
    mContext = context;
  }

  public synchronized static CleanService getInstance() {
    if (instance == null) {
      instance = new CleanService(MApp.getInstance());
    }
    return instance;
  }

  public Observable<String> checkAndStart() {
    return Observable.create(new Observable.OnSubscribe<String>() {

      @Override public void call(Subscriber<? super String> subscriber) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int month = calendar.get(Calendar.MONTH);
        calendar.set(Calendar.MONTH, month-3);
        try {
          List<Record> records = new Select().from(Record.class)
              .where("uploaded_datetime is not null and wk_date < ?", calendar.getTime().getTime())
              .orderBy("wk_date")
              .limit(1000)
              .execute();
          for (Record r : records) {
            r.delete();
          }
          subscriber.onNext("");
          subscriber.onCompleted();
        } catch (Exception e) {
          subscriber.onError(e);
        }
      }
    });
  }
}
