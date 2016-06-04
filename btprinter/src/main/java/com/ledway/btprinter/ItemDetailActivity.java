package com.ledway.btprinter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.serialport.api.SerialPort;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.Prod;
import com.zkc.Service.CaptureService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by togb on 2016/5/29.
 */
public class ItemDetailActivity extends AppCompatActivity {
  private SampleMaster mSampleMaster;
  private RecyclerView mListViewProd;
  private List<Map<String, String>> mProdList = new ArrayList<>();
  private DataAdapter mDataAdapter;
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
    mSampleMaster.prods.add(new Prod(text));
    TextData textData = new TextData(DataAdapter.DATA_TYPE_BARCODE);
    textData.setText(text);
    mDataAdapter.addData(textData);

  }


  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSampleMaster = (SampleMaster) getIntent().getSerializableExtra("cust_record");
    setContentView(R.layout.activity_item_detail);
    mListViewProd = (RecyclerView)findViewById(R.id.list_data);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("com.zkc.scancode");
    registerReceiver(scanBroadcastReceiver, intentFilter);
    setListView();
    setEvent();

  }

  private void setListView() {
    mDataAdapter = new DataAdapter(this);
    mListViewProd.setLayoutManager(new LinearLayoutManager(this));
    mListViewProd.setAdapter(mDataAdapter);
    mListViewProd.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
      @Override public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
      }

      @Override public void onTouchEvent(RecyclerView rv, MotionEvent e) {

      }

      @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

      }
    });

  }

  @Override protected void onDestroy() {
    super.onDestroy();
    unregisterReceiver(scanBroadcastReceiver);
  }

  private void setEvent() {
    findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
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

