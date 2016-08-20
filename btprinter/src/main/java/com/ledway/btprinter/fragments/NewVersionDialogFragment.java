package com.ledway.btprinter.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Created by togb on 2016/8/14.
 */
public class NewVersionDialogFragment extends DialogFragment {

  private CompositeSubscription mSubscriptions = new CompositeSubscription();
  private ProgressBar progressBar;
  private TextView txt_progress;
  private Button btn1;
  private Button btn2;
  private File downloadedFile;

  @NonNull @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    return dialog;
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.alert_dialog_fragment_new_version, container, false);
    TextView txtTitle = (TextView) view.findViewById(R.id.alertTitle);
    txtTitle.setText(R.string.title_new_version);
    btn1 = (Button) view.findViewById(android.R.id.button1);
    btn1.setText(R.string.btn_upgrade);
    btn1.setVisibility(View.VISIBLE);
    btn2 = (Button) view.findViewById(android.R.id.button2);
    TextView txtMessage = (TextView) view.findViewById(android.R.id.message);
    txtMessage.setText(getArguments().getString("desc"));
    txt_progress = (TextView) view.findViewById(R.id.txt_progress);
    progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
    progressBar.setMax(100);
    progressBar.setProgress(0);
    downloadedFile = new File(getApkPath(), getArguments().getString("apkName"));

    if (downloadedFile.exists() && downloadedFile.length() > 1000){
      btn1.setText(R.string.btn_install);
      progressBar.setVisibility(View.GONE);
    }
    boolean cancelable = getArguments().getBoolean("cancelable", true);
    if (cancelable){
      btn2.setText(R.string.btn_cancel);
      btn2.setVisibility(View.VISIBLE);
      btn2.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          SharedPreferences sp =
              getActivity().getSharedPreferences("upgrade", Context.MODE_PRIVATE);
          sp.edit().putBoolean(new SimpleDateFormat("yyyyMMdd").format(new Date()), true).apply();
          dismiss();
        }
      });
    }
    btn1.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        downloadInstall();
      }
    });
    return view;
  }

  private void downloadInstall() {
    if (downloadedFile.exists() && downloadedFile.length() > 1000){
                  Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(downloadedFile),
                "application/vnd.android.package-archive");
            startActivity(intent);
      return;
    }
    btn1.setEnabled(false);
    mSubscriptions.add( Observable.create(new Observable.OnSubscribe<Integer>() {
      @Override public void call(final Subscriber<? super Integer> subscriber) {
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(3, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build();

        final Request request =
            new Request.Builder().url( getArguments().getString("url")).build();
        subscriber.onNext(0);
        final Call call = client.newCall(request);
        call.enqueue(new Callback() {
          @Override public void onFailure(Call call, IOException e) {
            subscriber.onError(e);
          }
          @Override public void onResponse(Call call, Response response) throws IOException {
            long contentLen = response.body().contentLength();
            try {
              downloadedFile.getParentFile().mkdirs();
              File tmpFile = new File(downloadedFile.getAbsolutePath() + "_tmp");
              //        downloadedFile.createNewFile();
              BufferedSink sink = Okio.buffer(Okio.sink(tmpFile));
              BufferedSource source = response.body().source();
              byte[] buffer = new byte[8192];
              long totalBytesRead = 0;
              for (int readCount; (readCount = source.read(buffer)) != -1; ) {
                totalBytesRead += readCount;
                subscriber.onNext((int) (totalBytesRead * 100 / contentLen));
                sink.write(buffer, 0, readCount);
              }
              sink.close();
              tmpFile.renameTo(downloadedFile);
              subscriber.onCompleted();
            }catch (Exception e){
              subscriber.onError(e);
            }
/*            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(downloadedFile),
                "application/vnd.android.package-archive");
            startActivity(intent);*/
          }
        });

        subscriber.add(Subscriptions.create(new Action0() {
          @Override public void call() {
            call.cancel();
          }
        }));
      }
    }).onBackpressureBuffer().observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<Integer>() {
      @Override public void onCompleted() {
        btn1.setText(R.string.btn_install);
        btn1.setEnabled(true);
      }

      @Override public void onError(Throwable e) {
        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
        Log.e("download", e.getMessage(), e);
        btn1.setEnabled(true);
      }

      @Override public void onNext(Integer p) {
        progressBar.setProgress(p);
        txt_progress.setText(String.format("%d%%", p));
      }
    }));
  }

  @Override public void onDestroy() {
    super.onDestroy();
    mSubscriptions.clear();
  }

  public String getApkPath() {

    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
      return Environment.getExternalStorageDirectory() + "/tmp";
    } else {
      return Environment.getRootDirectory() + "/tmp";
    }
  }
}
