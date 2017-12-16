package com.ledway.btprinter.biz.main;

import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.activeandroid.Model;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gturedi.views.StatefulLayout;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.biz.sample.ReceivedSampleDetailActivity;
import com.ledway.btprinter.models.ReceivedSample;
import com.ledway.btprinter.models.Resource;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.btprinter.network.model.ProductAppGetReturn;
import com.ledway.btprinter.network.model.ProductReturn;
import com.ledway.btprinter.network.model.RestDataSetResponse;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class ReceiveSampleListFragment extends Fragment {
  @BindView(R.id.listview) RecyclerView mListView;
  @BindView(R.id.swiperefresh) SwipeRefreshLayout mSwipeRefresh;
  @BindView(R.id.statefulLayout) StatefulLayout mStatefulLayout;
  private CompositeDisposable mDisposables = new CompositeDisposable();
  private CompositeSubscription mSubscriptions = new CompositeSubscription();
  private Unbinder mViewBinder;
  private SampleListAdapter2 mSampleListAdapter;
  private MutableLiveData<Resource<List<SampleListAdapter2.ItemData>>> dataResource =
      new MutableLiveData<>();

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
    mSampleListAdapter = new SampleListAdapter2(getContext());
    loadFromCache();
    mDisposables.add(mSampleListAdapter.getClickObservable().subscribe(
        guid -> startActivity(new Intent(getContext(), ReceivedSampleDetailActivity.class).putExtra("guid", (String)guid))));
  }

  private void loadFromCache(){
    Observable.defer(() -> Observable.from(new Select().from(ReceivedSample.class).execute()))
        .map( cacheItem -> {
          ReceivedSample dbItem = (ReceivedSample) cacheItem;
          SampleListAdapter2.ItemData viewItem = new SampleListAdapter2.ItemData<>();
          viewItem.title = dbItem.title;
          viewItem.iconPath = dbItem.iconPath;
          viewItem.timestamp= dbItem.datetime;
          viewItem.hold = dbItem.holdId;
          viewItem.title = dbItem.title;
          return viewItem;
        }).toList().subscribe(new Subscriber<List<SampleListAdapter2.ItemData>>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        dataResource.postValue(Resource.error(e.getMessage(), null));
      }

      @Override public void onNext(List<SampleListAdapter2.ItemData> itemData) {
        if(itemData.size() ==0){
          loadFromRemoteData();
        }else {
          dataResource.postValue(Resource.success(itemData));
        }
      }
    });

  }

  private void loadFromRemoteData() {
    dataResource.postValue(Resource.loading(null));
    String query = "isnull(json,'') <>'' and shareToDeviceId like '" + MApp.getApplication()
        .getSystemInfo()
        .getDeviceId() + "%'";
//    query = "isnull(json,'') <>'' and shareToDeviceId like '" + "becb8da230e0a7cb" + "%'";
    String orderBy = "order by UPDATEDATE desc";
    new Delete().from(ReceivedSample.class).execute();

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Observable<RestDataSetResponse<ProductAppGetReturn>> obResponse =
        MyProjectApi.getInstance().getDbService().getProductAppGet(query, orderBy);
    mSubscriptions.add(obResponse.subscribeOn(Schedulers.io()).flatMap(response -> {

      ArrayList<ProductAppGetReturn> datasetResponse = response.result.get(0);
      ArrayList<SampleMaster> ret = new ArrayList<>();
      for (ProductAppGetReturn item : datasetResponse) {
        String json = item.json;
        try {
          SampleMaster sampleMaster = objectMapper.readValue(json, SampleMaster.class);
          ret.add(sampleMaster);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return Observable.from(ret);
    }).flatMap(sampleMaster -> {
      SampleListAdapter2.ItemData itemData = new SampleListAdapter2.ItemData();
      itemData.timestamp = sampleMaster.update_date;
      String title = sampleMaster.dataFrom;
      int index = title.indexOf('|');
      if (index > 0) {
        title = title.substring(index + 1);
      }
      itemData.title = title.trim();
      itemData.hold = sampleMaster.guid;
      ReceivedSample cached = new ReceivedSample();
      return Observable.from(sampleMaster.sampleProdLinks)
          .flatMap(sampleProdLink -> loadProductImage(sampleProdLink.prod_id))
          .toList()
          .map(files -> {
            if (!files.isEmpty()) {
              itemData.iconPath = files.get(0).getAbsolutePath();
            }
            try {
              cached.detailJson = objectMapper.writeValueAsString(sampleMaster.sampleProdLinks);
            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }
            cached.iconPath = itemData.iconPath;
            cached.title = itemData.title;
            cached.datetime = itemData.timestamp;
            cached.holdId = sampleMaster.guid;
            cached.save();
            return itemData;
          });
    }).toList().subscribe(new Subscriber<List<SampleListAdapter2.ItemData>>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        dataResource.postValue(Resource.error(e.getMessage(), null));
      }

      @Override public void onNext(List<SampleListAdapter2.ItemData> itemData) {
        dataResource.postValue(Resource.success(itemData));
      }
    }));
  }

  private Observable<File> loadProductImage(String prodno) {
    File imgFile = new File(MApp.getApplication().getPicPath() + "/product/" + prodno + ".png");
    if (!imgFile.getParentFile().exists()) {
      imgFile.getParentFile().mkdir();
    }
    String query = "prodno='" + prodno + "'";
    return MyProjectApi.getInstance()
        .getDbService()
        .getProduct(query, "")
        .subscribeOn(Schedulers.io())
        .flatMap(response -> {
          ArrayList<ProductReturn> list = response.result.get(0);
          for (ProductReturn productReturn : list) {
            if (!TextUtils.isEmpty(productReturn.graphic)) {
              byte[] data = Base64.decode(productReturn.graphic, Base64.DEFAULT);
              try (OutputStream stream = new FileOutputStream(imgFile)) {
                stream.write(data);
                return Observable.just(imgFile);
              } catch (FileNotFoundException e) {
                e.printStackTrace();
                return Observable.error(e);
              } catch (IOException e) {
                e.printStackTrace();
                return Observable.error(e);
              }
            }
          }
          return Observable.empty();
        });
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_sample_list, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mViewBinder = ButterKnife.bind(this, view);
    LinearLayoutManager layoutManager =
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    mListView.setLayoutManager(layoutManager);
    mListView.setAdapter(mSampleListAdapter);
    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(getActivity(), layoutManager.getOrientation());
    mListView.addItemDecoration(dividerItemDecoration);
    mSwipeRefresh.setOnRefreshListener(this::loadFromRemoteData);
    initView();
  }

  private void initView() {
    dataResource.observe(this, resource -> {
      switch (resource.status) {
        case LOADING: {
          mSwipeRefresh.setRefreshing(true);
          break;
        }
        case ERROR: {
          mSwipeRefresh.setRefreshing(false);
          Toast.makeText(getContext(), resource.message, Toast.LENGTH_LONG).show();
          break;
        }
        case SUCCESS: {
          mSwipeRefresh.setRefreshing(false);
          mSampleListAdapter.setData(resource.data);
          mSampleListAdapter.notifyDataSetChanged();
          if(resource.data.isEmpty()){
            mStatefulLayout.showEmpty();
          }else {
            mStatefulLayout.showContent();
          }
        }
      }
    });
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    mViewBinder.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    mDisposables.clear();
    mSubscriptions.clear();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    //inflater.inflate(R.menu.menu_sample_list, menu);
  }
}
