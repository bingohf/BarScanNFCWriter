package com.ledway.btprinter.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.activeandroid.util.Log;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.SampleReadonlyActivity;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.framework.RemoteDB;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/9/4.
 */
public class ReceiveSampleFragment extends PagerFragment{
  private RecordAdapter mRecordAdapter;
  private RemoteDB remoteDB = RemoteDB.getDefault();

  @Override public String getTitle() {
    return MApp.getApplication().getString(R.string.title_receive_sample);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_receive_sample, container, false);
    initView(view);
    return view;
  }

  private void initView(View view) {
    ListView listView = (ListView) view;
    mRecordAdapter = new RecordAdapter(getActivity());
    listView.setAdapter(mRecordAdapter);
    loadData();
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SampleMaster sampleMaster = mRecordAdapter.getItem(position);
        MApp.getApplication().getSession().put("current_data", sampleMaster);
        startActivity(new Intent(getActivity(), SampleReadonlyActivity.class));
      }
    });
  }

  private void loadData() {
    remoteDB.executeQuery("select a.json, b.CardPic "
        + " from PRODUCTAPPGET a left join CUSTOMER b on b.custno = a.custno  "
        +" where a.json <>? and a.json <>''", MApp.getApplication().getSystemInfo().getDeviceId())
        .subscribeOn(Schedulers.io())

        .flatMap(new Func1<ResultSet, Observable<SampleMaster>>() {
          @Override public Observable<SampleMaster> call(final ResultSet resultSet) {
            return Observable.create(new Observable.OnSubscribe<SampleMaster>() {
              @Override public void call(Subscriber<? super SampleMaster> subscriber) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                try {
                  while(resultSet.next()){
                    String json = resultSet.getString("json");

                    SampleMaster sampleMaster = objectMapper.readValue(json, SampleMaster.class);
                    File photoFile = new File(MApp.getApplication().getPicPath()
                        + "/"
                        + sampleMaster.guid
                        + "_type_"
                        + 1
                        + ".jpeg");
                    sampleMaster.image1 = null;
                    sampleMaster.image2 = null;
                    if(!photoFile.exists()){
                      InputStream inputStream = resultSet.getBinaryStream("CardPic");
                      if (inputStream != null && inputStream.available() >0){
                        FileOutputStream outputStream =
                            new FileOutputStream(photoFile);
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = inputStream.read(buffer)) != -1) {
                          outputStream.write(buffer, 0, read);
                        }
                        outputStream.flush();
                        outputStream.close();
                        sampleMaster.image1 = photoFile.getAbsolutePath();
                      }
                    }else {
                      sampleMaster.image1 = photoFile.getAbsolutePath();
                    }


                    subscriber.onNext(sampleMaster);
                  }
                  subscriber.onCompleted();
                } catch (Exception e) {
                  e.printStackTrace();
                  subscriber.onError(e);
                }
              }
            });
          }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<SampleMaster>() {
          @Override public void onCompleted() {
            mRecordAdapter.notifyDataSetChanged();
          }

          @Override public void onError(Throwable e) {
            Log.e("error", e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
          }

          @Override public void onNext(SampleMaster sampleMaster) {
            mRecordAdapter.addData(sampleMaster);

          }
        });
  }
}
