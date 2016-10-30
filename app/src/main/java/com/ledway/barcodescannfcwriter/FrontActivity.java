package com.ledway.barcodescannfcwriter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Toast;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/5/14.
 */
public class FrontActivity extends AppCompatActivity {
  private SharedPreferences mSp;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_front);
    getSupportActionBar().setTitle(R.string.license);
    TextView tv = (TextView) findViewById(R.id.txt_license);
    tv.setMovementMethod(LinkMovementMethod.getInstance());
    mSp = getSharedPreferences("license", Context.MODE_PRIVATE);
    if (BuildConfig.DEBUG||mSp.getBoolean("authority", false) == true && mSp.getLong("ExpiredDate",0L) > new Date().getTime()){
      finish();
      startActivity(new Intent(this, MainActivity.class));
    }else{
      WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
      WifiInfo info = manager.getConnectionInfo();
      String address = info.getMacAddress();
      String appVersion = BuildConfig.APPLICATION_ID + "_" + BuildConfig.VERSION_NAME;
      final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.authorization), getString(R.string.wait_a_moment), false);
      MApp.getInstance().getLicenseDB().executeProcedure("{call sp_queryLicense(?,?,?,?)}",new int[]{
          Types.INTEGER,Types.TIMESTAMP}, address, appVersion)
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Subscriber<ArrayList<Object>>() {
            @Override public void onCompleted() {
              progressDialog.dismiss();
            }

            @Override public void onError(Throwable e) {
              progressDialog.dismiss();
              Toast.makeText(FrontActivity.this, e.getMessage(),Toast.LENGTH_LONG).show();
              e.printStackTrace();
            }

            @Override public void onNext(ArrayList<Object> objects) {
              int active = (int) objects.get(0);
              long expiredDate =0;
              if (objects.get(1) != null){
                expiredDate = ((java.sql.Timestamp)objects.get(1)).getTime();
              }

              if (active >0 && expiredDate > new Date().getTime()){
                finish();
                startActivity(new Intent(FrontActivity.this, MainActivity.class));
              }

              mSp.edit().putBoolean("authority", active >0)
                  .putLong("ExpiredDate", expiredDate).commit();
            }
          });



    }
  }
}
