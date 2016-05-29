package com.ledway.btprinter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import com.ledway.framework.RemoteDB;
import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {
  private RemoteDB remoteDB;
  private PublishSubject<Boolean> mSettingSubject = PublishSubject.create();
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mSettingSubject.subscribe(new Action1<Boolean>() {
      @Override public void call(Boolean aBoolean) {
        if (aBoolean){
          reset();
        }else {
          AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
          builder.setTitle(R.string.invalid_setting)
              .setMessage(R.string.goto_setting)
              .setPositiveButton(R.string.setting, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  Intent intent = new Intent(MainActivity.this, AppPreferences.class);
                  startActivityForResult(intent,1);
                }
              })
              .setCancelable(false);
          builder.create().show();
        }
      }
    });
    checkSetting();
  }

  private void reset() {

  }

  private void checkSetting(){
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    String line = sp.getString("Line", "01");
    String reader = sp.getString("Reader", "01");
    String server = sp.getString("Server", "vip.ledway.com.tw");
    if (TextUtils.isEmpty(line) || TextUtils.isEmpty(reader) || TextUtils.isEmpty(server)){
      mSettingSubject.onNext(false);
    }else{
      mSettingSubject.onNext(true);
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    checkSetting();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.action_add:{
        startActivity(new Intent(this, ItemDetailActivity.class));
        break;
      }
    }
    return true;
  }
}
