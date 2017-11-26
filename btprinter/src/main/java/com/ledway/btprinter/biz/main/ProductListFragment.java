package com.ledway.btprinter.biz.main;

import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.activeandroid.query.Select;
import com.gturedi.views.StatefulLayout;
import com.ledway.btprinter.R;
import com.ledway.btprinter.TodoProdDetailActivity;
import com.ledway.btprinter.models.Resource;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.framework.FullScannerActivity;
import java.util.List;
import rx.Observable;
import rx.Subscriber;

public class ProductListFragment extends Fragment {
  private static final int RESULT_CAMERA_QR_CODE = 1;
  @BindView(R.id.listview) RecyclerView mListView;
  @BindView(R.id.swiperefresh) SwipeRefreshLayout mSwipeRefresh;
  @BindView(R.id.statefulLayout) StatefulLayout mStatefulLayout;
  private Unbinder mViewBinder;
  private SampleListAdapter2 mSampleListAdapter;
  private MutableLiveData<Resource<List<SampleListAdapter2.ItemData>>> dataResource =
      new MutableLiveData<>();

  public ProductListFragment() {
    setRetainInstance(true);
    setHasOptionsMenu(true);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case RESULT_CAMERA_QR_CODE: {
        if (resultCode == Activity.RESULT_OK) {
          String qrcode = data.getStringExtra("barcode");
          startActivityForResult(
              new Intent(getActivity(), TodoProdDetailActivity.class).putExtra("prod_no", qrcode),
              1);
        }
        break;
      }
    }
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mSampleListAdapter = new SampleListAdapter2(getContext());
    loadData();
  }

  private void loadData() {
    Observable.defer(() -> Observable.from(new Select().from(TodoProd.class).execute())).map(o -> {
      TodoProd todoProd = (TodoProd) o;
      SampleListAdapter2.ItemData itemData = new SampleListAdapter2.ItemData();
      itemData.title = todoProd.spec_desc;
      itemData.timestamp = todoProd.created_time;
      itemData.subTitle = todoProd.prodNo;
      itemData.title = todoProd.spec_desc;
      itemData.hold = todoProd.prodNo;
      itemData.iconPath = todoProd.image1;
      return itemData;
    }).toList().subscribe(new Subscriber<List<SampleListAdapter2.ItemData>>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        dataResource.postValue(Resource.error(e.getMessage(), null));
      }

      @Override public void onNext(List<SampleListAdapter2.ItemData> itemData) {
        dataResource.postValue(Resource.success(itemData));
      }
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
    mSwipeRefresh.setEnabled(false);
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
          if (resource.data.isEmpty()) {
            mStatefulLayout.showEmpty();
          } else {
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

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_sample_list, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_add: {
        scanBarCode();

        break;
      }
    }
    return true;
  }

  private void scanBarCode() {
    startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
        RESULT_CAMERA_QR_CODE);
  }
}
