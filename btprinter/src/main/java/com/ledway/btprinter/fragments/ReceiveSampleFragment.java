package com.ledway.btprinter.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
import com.ledway.btprinter.adapters.ReceiveSampleAdapter;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.framework.RemoteDB;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/9/4.
 */
public class ReceiveSampleFragment extends PagerFragment{
  private SimpleAdapter simpleAdapter;
  private ArrayList<HashMap<String,String>> dataList = new ArrayList<>();
  private ArrayList<SampleMaster> sampleList = new ArrayList<>();
  private RemoteDB remoteDB = RemoteDB.getDefault();
  private SwipeRefreshLayout swipeRefreshLayout;

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
    ListView listView = (ListView) view.findViewById(R.id.list_view);
    swipeRefreshLayout =
        (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
    simpleAdapter = new ReceiveSampleAdapter(getActivity(),dataList, R.layout.list_item_record, new String[]{"text", "text2"}, new int[]{R.id.text1, R.id.text2});
    listView.setAdapter(simpleAdapter);
    loadData();
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SampleMaster sampleMaster = sampleList.get(position);
        MApp.getApplication().getSession().put("current_data", sampleMaster);
        startActivity(new Intent(getActivity(), SampleReadonlyActivity.class));
      }
    });
    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override public void onRefresh() {
        loadData();
      }
    });
  }

  private void loadData() {
    swipeRefreshLayout.setRefreshing(true);
    dataList.clear();
    sampleList.clear();
    simpleAdapter.notifyDataSetChanged();
    remoteDB.executeQuery("select a.json, b.CardPic "
        + " from PRODUCTAPPGET a left join CUSTOMER b on b.custno = a.custno  "
        +" where a.shareToDeviceId like ? and a.json <>'' order by a.CREATEDATE desc", MApp.getApplication().getSystemInfo().getDeviceId() +"%")
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
            simpleAdapter.notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
          }

          @Override public void onError(Throwable e) {
            Log.e("error", e.getMessage(), e);
            e.printStackTrace();
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
          }

          @Override public void onNext(SampleMaster sampleMaster) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            String text = sampleMaster.create_date.toLocaleString();
            text += (TextUtils.isEmpty(sampleMaster.getDesc())?"": "\r\n" +sampleMaster.getDesc());
            hashMap.put("text",text);

            if (!TextUtils.isEmpty(sampleMaster.dataFrom)){
              hashMap.put("text2",sampleMaster.dataFrom.trim().replaceAll("\\r|\\n", " "));
            }
            dataList.add(hashMap);
            sampleList.add(sampleMaster);

          }
        });
  }
}
