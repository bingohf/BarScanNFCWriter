package com.ledway.btprinter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import java.util.Iterator;

/**
 * Created by togb on 2016/9/11.
 */
public class SampleReadonlyActivity extends AppCompatActivity {
  private RecyclerView mListViewProd;
  private DataAdapter mDataAdapter;
  private SampleMaster mSampleMaster;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSampleMaster = (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
    setContentView(R.layout.activity_sample_readonly);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    initView();
  }

  private void initView() {
    mListViewProd = (RecyclerView) findViewById(R.id.list_data);
    mDataAdapter = new DataAdapter(this);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    mListViewProd.setLayoutManager(linearLayoutManager);
    mListViewProd.setAdapter(mDataAdapter);


    if (!TextUtils.isEmpty(mSampleMaster.getDesc())) {
      TextData textData = new TextData(DataAdapter.DATA_TYPE_MEMO);
      textData.setText(mSampleMaster.getDesc());
      mDataAdapter.addData(textData);
    }
    if (!TextUtils.isEmpty(mSampleMaster.getImage1())) {
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_PHOTO_1);
      photoData.setBitmapPath(mSampleMaster.getImage1());
      mDataAdapter.addData(photoData);
    }

    if (!TextUtils.isEmpty(mSampleMaster.getImage2())) {
      PhotoData photoData = new PhotoData(DataAdapter.DATA_TYPE_PHOTO_2);
      photoData.setBitmapPath(mSampleMaster.getImage2());
      mDataAdapter.addData(photoData);
    }

    Iterator<SampleProdLink> iterator = mSampleMaster.prodIterator();
    while (iterator.hasNext()) {
      SampleProdLink prod = iterator.next();
      TextData textData = new TextData(DataAdapter.DATA_TYPE_BARCODE);
      textData.setText(prod.ext + ": " + prod.prod_id + "  " + prod.spec_desc);
      mDataAdapter.addData(textData);
    }

  }
}
