package com.ledway.btprinter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import com.ledway.btprinter.adapters.DataAdapter;

/**
 * Created by togb on 2016/9/11.
 */
public class SampleReadonlyActivity extends AppCompatActivity {
  private RecyclerView mListViewProd;
  private DataAdapter mDataAdapter;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sample_readonly);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    initView();
  }

  private void initView() {
    mListViewProd = (RecyclerView) findViewById(R.id.list_data);
    mDataAdapter = new DataAdapter(this);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    linearLayoutManager.setStackFromEnd(true);
    mListViewProd.setLayoutManager(linearLayoutManager);
    mListViewProd.setAdapter(mDataAdapter);
  }
}
