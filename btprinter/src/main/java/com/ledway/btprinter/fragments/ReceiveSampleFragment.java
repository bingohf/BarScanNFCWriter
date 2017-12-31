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
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.SampleReadonlyActivity;
import com.ledway.btprinter.adapters.ReceiveSampleAdapter;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.btprinter.network.model.ProductAppGetReturn;
import com.ledway.btprinter.network.model.RestDataSetResponse;
import com.ledway.btprinter.utils.JsonUtils;
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
public class ReceiveSampleFragment extends PagerFragment {
  private SimpleAdapter simpleAdapter;
  private ArrayList<HashMap<String, String>> dataList = new ArrayList<>();
  private ArrayList<SampleMaster> sampleList = new ArrayList<>();
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
    swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
    simpleAdapter = new ReceiveSampleAdapter(getActivity(), dataList, R.layout.list_item_record,
        new String[] { "text", "text2" }, new int[] { R.id.text1, R.id.text2 });
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
    String query = "isnull(json,'') <>'' and shareToDeviceId like '" + MApp.getApplication()
        .getSystemInfo()
        .getDeviceId() + "%'";
    String orderBy = "order by UPDATEDATE desc";

    Observable<RestDataSetResponse<ProductAppGetReturn>> obResponse =
        MyProjectApi.getInstance().getDbService().getProductAppGet(query, orderBy);

    obResponse.subscribeOn(Schedulers.io())
        .flatMap(new Func1<RestDataSetResponse<ProductAppGetReturn>, Observable<SampleMaster>>() {
          @Override public Observable<SampleMaster> call(
              final RestDataSetResponse<ProductAppGetReturn> response) {
            return Observable.create(new Observable.OnSubscribe<SampleMaster>() {
              @Override public void call(Subscriber<? super SampleMaster> subscriber) {
                ArrayList<ProductAppGetReturn> datasetResponse = response.result.get(0);
                try {
                  for (ProductAppGetReturn item : datasetResponse) {
                    String json = item.json;
                    SampleMaster sampleMaster = JsonUtils.Companion.fromJson(json, SampleMaster.class);
/*                    File photoFile = new File(MApp.getApplication().getPicPath()
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
                    }*/
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
            text +=
                (TextUtils.isEmpty(sampleMaster.getDesc()) ? "" : "\r\n" + sampleMaster.getDesc());
            hashMap.put("text", text);

            if (!TextUtils.isEmpty(sampleMaster.dataFrom)) {
              hashMap.put("text2", sampleMaster.dataFrom.trim().replaceAll("\\r|\\n", " "));
            }
            dataList.add(hashMap);
            sampleList.add(sampleMaster);
          }
        });
  }
}
