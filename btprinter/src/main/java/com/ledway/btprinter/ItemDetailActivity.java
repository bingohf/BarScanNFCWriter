package com.ledway.btprinter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.serialport.api.SerialPort;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.ledway.btprinter.models.CustRecord;
import com.ledway.btprinter.models.Prod;
import com.zkc.Service.CaptureService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import rx.Observable;
import rx.functions.Action1;

/**
 * Created by togb on 2016/5/29.
 */
public class ItemDetailActivity extends AppCompatActivity {
  private CustRecord mCustRecord;
  private ListView mListViewProd;
  private SimpleAdapter mSimpleAdapter;
  private List<Map<String, String>> mProdList = new ArrayList<>();
  private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver(){

    @Override
    public void onReceive(Context context, Intent intent) {
      String text = intent.getExtras().getString("code");
      if (text.length() < 10){
        Toast.makeText(ItemDetailActivity.this, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
      }
      Pattern pattern = Pattern.compile("[^0-9a-zA-Z_ ]");
      if(!pattern.matcher(text).matches()) {
        appendBarCode(text);
      }
    }

  };


  private void appendBarCode(String text) {
    mCustRecord.prods.add(new Prod(text));
    HashMap<String, String> data = new HashMap<>();
    data.put("text",text);
    mProdList.add(data);
    mSimpleAdapter.notifyDataSetChanged();

  }


  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mCustRecord = (CustRecord) getIntent().getSerializableExtra("cust_record");
    setContentView(R.layout.activity_item_detail);
    mListViewProd = (ListView)findViewById(R.id.list_prod);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("com.zkc.scancode");
    registerReceiver(scanBroadcastReceiver, intentFilter);
    setListView();
    setEvent();

  }

  private void setListView() {
    mSimpleAdapter = new SimpleAdapter(this, mProdList, android.R.layout.simple_list_item_1, new String[]{"text"},new int[]{android.R.id.text1});
    mListViewProd.setAdapter(mSimpleAdapter);

  }

  @Override protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(scanBroadcastReceiver);
  }

  private void setEvent() {
    findViewById(R.id.btn_open_scan).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        SerialPort.CleanBuffer();
        CaptureService.scanGpio.openScan();
      }
    });
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.item_detail_menu,menu);
    return true;

  }
}

