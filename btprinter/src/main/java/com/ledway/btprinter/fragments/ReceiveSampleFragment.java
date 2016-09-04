package com.ledway.btprinter.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.activeandroid.util.Log;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.framework.RemoteDB;
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
    remoteDB.executeQuery("select a.series guid,"
        + " a.salesno mac_address,"
        + "a.updatedate create_date,"
        + " a.updatedate update_date,"
        + "b.memo [desc] "
        + " from PRODUCTAPPGET a "
        + " join CUSTOMER b on a.custno = b.custno where a.shareTodeviceId=?", MApp.getApplication().getSystemInfo().getDeviceId())
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
              while(resultSet.next()){
                SampleMaster sampleMaster = new SampleMaster();
                sampleMaster.guid = resultSet.getString("guid");
                sampleMaster.create_date = resultSet.getDate("create_date");
                sampleMaster.update_date = resultSet.getDate("update_date");
                sampleMaster.mac_address = resultSet.getString("mac_address");
                sampleMaster.desc = resultSet.getString("desc");
                sampleMaster.isDirty = false;
               mRecordAdapter.addData(sampleMaster);
              }
              mRecordAdapter.notifyDataSetChanged();
            } catch (SQLException e) {
              e.printStackTrace();
            }
          }
        });
  }
}
