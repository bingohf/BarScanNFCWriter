package com.ledway.barcodescannfcwriter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.ledway.barcodescannfcwriter.models.Record;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by togb on 2016/4/23.
 */
public class UploadService {
    private Context context;
    public UploadService(Context context){
        this.context = context;
    }


    public Observable<String> uploadRecord(Record record){
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (!isOnline()){
                    subscriber.onError(new Exception("Network is not available"));
                }
            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
