package com.ledway.barcodescannfcwriter;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.ledway.barcodescannfcwriter.models.Record;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/4/10.
 */
public class UploadPreference extends Preference {
    private int mRemainCount;
    private static final DateFormat dateFormat = new SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss");
    private View progressView;
    private TextView txtSummary;

    public UploadPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UploadPreference(Context context, AttributeSet attrs) {
        this(context, attrs ,0);
    }

    public UploadPreference(Context context) {
        this(context, null);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
       // return super.onCreateView(parent);
    //    LayoutInflater li = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
       // return li.inflate( R.layout.preference_upload, parent, false);
        View view = LayoutInflater.from(getContext()).inflate(
                R.layout.preference_upload, parent, false);
        return view;
    }
    private  void loadRemainRecord(){
        Observable<Integer> total = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                String sql = "SELECT COUNT(*) as total FROM record where uploaded_datetime is null";
                Cursor c = ActiveAndroid.getDatabase().rawQuery(sql, null);
                c.moveToFirst();
                int total = c.getInt(0);
                subscriber.onNext(total);
                subscriber.onCompleted();
            }
        });
        progressView.setVisibility(View.VISIBLE);
        total.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        progressView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(),e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Integer integer) {
                        mRemainCount = integer;
                        txtSummary.setText("待上传的资料有 " + integer.toString() +" 笔");
                    }
                });
    }


    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
         txtSummary = (TextView) view.findViewById(android.R.id.summary);
        progressView = view.findViewById(R.id.progressBar);
        loadRemainRecord();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog progressDialog = new ProgressDialog(getContext());
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setIndeterminate(false);
                progressDialog.setMax(mRemainCount);
                progressDialog.setMessage(getContext().getString(R.string.uploading));
                progressDialog.setCancelable(false);
                progressDialog.show();

                MApp.getInstance().getSettings().reload();
                Observable.create(new Observable.OnSubscribe<Record>() {
                    @Override
                    public void call(Subscriber<? super Record> subscriber) {
                        List<Record> records = new Select().from(Record.class).where("uploaded_datetime is null").orderBy("wk_date").execute();
                        for(Record record: records){
                            subscriber.onNext(record);
                        }
                        subscriber.onCompleted();
                    }
                }).flatMap(new Func1<Record, Observable<String>>() {
                    @Override
                    public Observable<String> call(Record record) {
                        return MApp.getInstance().getUploadService().uploadRecord(record);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        progressDialog.dismiss();
                        loadRemainRecord();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onNext(String msg) {
                        progressDialog.setProgress(progressDialog.getProgress() + 1);
                    }
                });



            }
        });

    }
}
