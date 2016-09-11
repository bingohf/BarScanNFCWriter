package com.ledway.btprinter.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.activeandroid.util.Log;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.framework.RemoteDB;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
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
    RecordAdapter.setSingletonInstance(mRecordAdapter);
    loadData();
  }

  private void loadData() {
    remoteDB.executeQuery("select a.json "
        + " from PRODUCTAPPGET a "
        +" where a.json <>? and a.json <>''", MApp.getApplication().getSystemInfo().getDeviceId())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<ResultSet>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            Log.e(e.getMessage(),e);
          }

          @Override public void onNext(ResultSet resultSet) {
            try {
              ObjectMapper objectMapper = new ObjectMapper();
              objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
              objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
              while(resultSet.next()){
                String json = resultSet.getString("json");
                SampleMaster sampleMaster = objectMapper.readValue(json, SampleMaster.class);
               mRecordAdapter.addData(sampleMaster);
              }
              mRecordAdapter.notifyDataSetChanged();
            } catch (SQLException e) {
              e.printStackTrace();
            } catch (JsonParseException e) {
              e.printStackTrace();
            } catch (JsonMappingException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        });
  }
}
