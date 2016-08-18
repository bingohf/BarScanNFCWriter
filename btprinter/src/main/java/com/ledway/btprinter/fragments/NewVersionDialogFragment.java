package com.ledway.btprinter.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.ledway.btprinter.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by togb on 2016/8/14.
 */
public class NewVersionDialogFragment extends DialogFragment {


  private CompositeSubscription mSubscriptions = new CompositeSubscription();
  private ProgressBar progressBar;
  private TextView txt_progress;

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog =  super.onCreateDialog(savedInstanceState);
    dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.alert_dialog_fragment_new_version, container,false);
    TextView txtTitle  = (TextView) view.findViewById(R.id.alertTitle);
    txtTitle.setText(R.string.title_new_version);
    Button btn1 = (Button) view.findViewById(android.R.id.button1);
    btn1.setText(R.string.btn_install);
    btn1.setVisibility(View.VISIBLE);
    txt_progress = (TextView)view.findViewById(R.id.txt_progress);
    progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
    progressBar.setMax(100);
    progressBar.setProgress(0);

    btn1.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startDownLoad();
      }
    });
    return view;
  }

  private void startDownLoad() {
    Observable.create(new Observable.OnSubscribe<Integer>() {
      @Override public void call(Subscriber<? super Integer> subscriber) {
        OkHttpClient client =  new OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(3,TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES).build();
        Request request =
            new Request.Builder().url("http://www.ledway.com.tw/uploads/sales_edge.apk")
                .build();
        try {
          Response response = client.newCall(request).execute();

          float contentLength = response.body().contentLength();
          File downloadedFile = new File(getContext().getExternalCacheDir(), "aaaa.apk");
          FileOutputStream fileOutputStream = new FileOutputStream(downloadedFile);
          byte[] buffer = new byte[100];
          int read = 0;
          subscriber.onNext(0);
          BufferedSink sink = Okio.buffer(Okio.sink(downloadedFile));
          Buffer sinkBuffer = sink.buffer();
          BufferedSource source = response.body().source();;
          long totalBytesRead = 0;
          int bufferSize = 8 * 1024;
          for (long bytesRead; (bytesRead = source.read(sinkBuffer, bufferSize)) != -1; ) {
            sink.emit();
            totalBytesRead += bytesRead;
            int progress = (int) ((totalBytesRead * 100) / contentLength);
            subscriber.onNext(progress);
          }
          sink.flush();
          sink.close();
          source.close();
/*          int count = 0;
          while ((read = source.read(buffer)) > 0){
            fileOutputStream.write(buffer,0, read);
            count += read;
            subscriber.onNext( count/  fileSize);
          }
          fileOutputStream.flush();
          fileOutputStream.close();
          source.close();*/
          subscriber.onCompleted();
        } catch (IOException e) {
          e.printStackTrace();
          subscriber.onError(e);
          Log.e("xxxxx",e.getMessage(),e);
        }
      }
    })
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Integer>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        Toast.makeText(getActivity(), e.getMessage(),Toast.LENGTH_LONG).show();
      }

      @Override public void onNext(Integer p) {
        progressBar.setProgress(p);
        txt_progress.setText(String.format("%d%%", p));
      }
    });



  }

  @Override public void onDestroy() {
    super.onDestroy();
    mSubscriptions.clear();
  }
}
