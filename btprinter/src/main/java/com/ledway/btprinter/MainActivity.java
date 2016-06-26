package com.ledway.btprinter;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.fragments.BindBTPrintDialogFragment;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.framework.RemoteDB;
import com.zkc.Service.CaptureService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {
  private final static int REQUEST_TYPE_SETTING = 1;
  private final static int REQUEST_TYPE_ADD_RECORD = 2;
  private final static int REQUEST_TYPE_MODIFY_RECORD = 3;
  private RemoteDB remoteDB;
  private PublishSubject<Boolean> mSettingSubject = PublishSubject.create();
  private RecordAdapter mRecordAdapter;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    Intent newIntent = new Intent(MainActivity.this, CaptureService.class);
    newIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP );
    startService(newIntent);
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
                  startActivityForResult(intent,REQUEST_TYPE_SETTING);
                }
              })
              .setCancelable(false);
          builder.create().show();
        }
      }
    });
    checkSetting();



    findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        SampleMaster sampleMaster = new SampleMaster();
        MApp.getApplication().getSession().put("current_data",sampleMaster);
        startActivityForResult(new Intent(MainActivity.this, ItemDetailActivity.class),REQUEST_TYPE_ADD_RECORD);
      }
    });

    setListView();
  }

  private void setListView() {
    ListView listView = (ListView) findViewById(R.id.list_record);
    mRecordAdapter = new RecordAdapter(this);
    listView.setAdapter(mRecordAdapter);
    getRecordData();
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SampleMaster sampleMaster = mRecordAdapter.getItem(position);
        MApp.getApplication().getSession().put("current_data",sampleMaster);
        startActivityForResult(new Intent(MainActivity.this, ItemDetailActivity.class), REQUEST_TYPE_MODIFY_RECORD);
      }
    });
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
    SampleMaster currentData = (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
    switch (requestCode){
      case REQUEST_TYPE_SETTING:{
        checkSetting();
        break;
      }
      case REQUEST_TYPE_ADD_RECORD:{
        if (currentData.isHasData()) {
          mRecordAdapter.addData(0, currentData);
        }
        break;
      }
      case REQUEST_TYPE_MODIFY_RECORD:{
        mRecordAdapter.moveToTop(currentData);
        break;
      }
    }

  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.action_upload:{
        uploadAll();
        break;
      }
      case  R.id.action_bind_bt_printer:{
        showSetDialog();
        break;
      }

    }
    return true;
  }

  private void showSetDialog() {
    BindBTPrintDialogFragment bindBTPrintDialogFragment = new BindBTPrintDialogFragment();
    bindBTPrintDialogFragment.show(getSupportFragmentManager(), "dialog");
  }

  private void uploadAll() {
    final ProgressDialog progressDialog = ProgressDialog.show(this,getString(R.string.upload), getString(R.string.wait_a_moment), false);
    Observable.from(mRecordAdapter).filter(new Func1<SampleMaster, Boolean>() {
      @Override public Boolean call(SampleMaster sampleMaster) {
        return ! sampleMaster.isUploaded();
      }
    }).flatMap(new Func1<SampleMaster, Observable<SampleMaster>>() {
      @Override public Observable<SampleMaster> call(SampleMaster sampleMaster) {
        return  sampleMaster.remoteSave();
      }
    }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<SampleMaster>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
            mRecordAdapter.notifyDataSetChanged();
          }

          @Override public void onError(Throwable e) {
            progressDialog.dismiss();
            Log.e("upload_all" , e.getMessage(), e);
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
          }

          @Override public void onNext(SampleMaster sampleMaster) {

          }
        });


  }

  private void getRecordData(){
    List<SampleMaster> dataList = new Select(new String[]{"create_date", "desc" ,"update_date","guid", "id","mac_address","isDirty","line","reader","qrcode"}).from(SampleMaster.class).orderBy(" update_date desc ").execute();
    mRecordAdapter.clear();
    for(SampleMaster sampleMaster:dataList){
      mRecordAdapter.addData(sampleMaster);
    }
  }



}
