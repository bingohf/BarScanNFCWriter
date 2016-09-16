package com.ledway.btprinter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import com.ledway.btprinter.adapters.DataAdapter;
import com.ledway.btprinter.adapters.PhotoData;
import com.ledway.btprinter.adapters.TextData;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import com.ledway.framework.RemoteDB;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/9/11.
 */
public class SampleReadonlyActivity extends AppCompatActivity {
  private RecyclerView mListViewProd;
  private DataAdapter mDataAdapter;
  private SampleMaster mSampleMaster;
  private RemoteDB remoteDB = RemoteDB.getDefault();

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSampleMaster = (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
    setContentView(R.layout.activity_sample_readonly);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    initView();
    loadData();
    loadProduct();
  }

  private void loadData() {
    mDataAdapter.clear();
    if (!TextUtils.isEmpty(mSampleMaster.dataFrom)) {
      TextData textData = new TextData(DataAdapter.DATA_TYPE_DATA_FROM);
      textData.setText(mSampleMaster.dataFrom);
      mDataAdapter.addData(textData);
    }
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

  private void loadProduct() {
    String sql = "select specdesc,prodno from product where empNo =? and  ( 1=2 ";
    int i = 1;
    Object[] objects = new Object[mSampleMaster.sampleProdLinks.size() +1];
    objects[0] = mSampleMaster.mac_address;
    for (SampleProdLink prodLink : mSampleMaster.sampleProdLinks) {
      objects[i] = prodLink.prod_id;
      sql += "or  prodNo =?";
      ++i;
    }
    sql += " )";
    remoteDB.executeQuery(sql, objects)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<ResultSet>() {
          @Override public void onCompleted() {
            loadData();
          }

          @Override public void onError(Throwable e) {
            Log.e("error", e.getMessage(), e);
          }

          @Override public void onNext(ResultSet resultSet) {
            try {
              while (resultSet.next()) {
                String desc = resultSet.getString("specdesc");
                String prodno = resultSet.getString("prodno");
                setProdDesc(prodno, desc);
              }
            } catch (SQLException e) {
              e.printStackTrace();
            }
          }
        });
  }

  private void setProdDesc(String prodno, String desc) {
    for(SampleProdLink sampleProdLink :mSampleMaster.sampleProdLinks){
      if(sampleProdLink.prod_id.equals(prodno)){
        sampleProdLink.spec_desc = desc;
        break;
      }
    }
  }

  private void initView() {

    mListViewProd = (RecyclerView) findViewById(R.id.list_data);
    mDataAdapter = new DataAdapter(this);
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    mListViewProd.setLayoutManager(linearLayoutManager);
    mListViewProd.setAdapter(mDataAdapter);


  }
}
