package com.ledway.btprinter.biz.main;

import android.app.ProgressDialog;
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
import com.ledway.btprinter.AppConstants;
import com.ledway.btprinter.ItemDetailActivity;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.models.Resource;
import com.ledway.btprinter.models.SampleMaster;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

public class SampleListFragment extends Fragment {
  @BindView(R.id.listview) RecyclerView mListView;
  @BindView(R.id.swiperefresh) SwipeRefreshLayout mSwipeRefresh;
  private MutableLiveData<Resource<List<SampleListAdapter2.ItemData>>> mResourceSampleList =
      new MutableLiveData<>();
  private Unbinder mViewBinder;
  private CompositeDisposable mDisposables = new CompositeDisposable();
  private CompositeDisposable mViewDisposables;
  private SampleListAdapter2 mSampleListAdapter;
  private ProgressDialog mProgressDialog;

  public SampleListFragment() {
    setRetainInstance(true);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case AppConstants.REQUEST_TYPE_ADD_RECORD:
      case AppConstants.REQUEST_TYPE_MODIFY_RECORD: {
        loadRecordData();
        break;
      }
    }
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_sample_list, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    mViewDisposables = new CompositeDisposable();
    mViewBinder = ButterKnife.bind(this, view);
    LinearLayoutManager layoutManager =
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    mListView.setLayoutManager(layoutManager);
    mSampleListAdapter = new SampleListAdapter2(view.getContext());
    mListView.setAdapter(mSampleListAdapter);
    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(getActivity(), layoutManager.getOrientation());
    mListView.addItemDecoration(dividerItemDecoration);
    initView();
  }

  @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
    //setReenterTransition(true);
    loadRecordData();
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    mViewBinder.unbind();
    stopLoading();
    mViewDisposables.clear();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    mDisposables.clear();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_sample_list, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_add: {
        SampleMaster sampleMaster = new SampleMaster();
        MApp.getApplication().getSession().put("current_data", sampleMaster);
        startActivityForResult(new Intent(getActivity(), ItemDetailActivity.class),
            AppConstants.REQUEST_TYPE_ADD_RECORD);
        break;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  private void initView() {
    mResourceSampleList.observe(this, listResource -> {
      switch (listResource.status) {
        case LOADING: {
          //showLoading();
          mSwipeRefresh.setRefreshing(true);
          break;
        }
        case SUCCESS: {
          mSwipeRefresh.setRefreshing(false);
          mSampleListAdapter.setData(listResource.data);
          mSampleListAdapter.notifyDataSetChanged();
          break;
        }
        case ERROR: {
          mSwipeRefresh.setRefreshing(false);
          Toast.makeText(getActivity(), listResource.message, Toast.LENGTH_LONG).show();
          break;
        }
      }
    });
    mViewDisposables.add(mSampleListAdapter.getClickObservable().subscribe(sampleMaster -> {
      MApp.getApplication().getSession().put("current_data", sampleMaster);
      startActivityForResult(new Intent(getActivity(), ItemDetailActivity.class),
          AppConstants.REQUEST_TYPE_MODIFY_RECORD);
    }));

    mSwipeRefresh.setOnRefreshListener(this::loadRecordData);
  }

  private void showLoading() {
    stopLoading();
    mProgressDialog = ProgressDialog.show(getActivity(), getString(R.string.upload),
        getString(R.string.wait_a_moment), false);
  }

  private void stopLoading() {
    if (mProgressDialog != null) {
      mProgressDialog.dismiss();
      mProgressDialog = null;
    }
  }

  private void loadRecordData() {
    mDisposables.add(Single.defer(() -> {
      List<SampleMaster> data =
          new Select().from(SampleMaster.class).orderBy(" create_date desc ").execute();
      return Single.just(data);
    })
        .doOnSubscribe(disposable -> mResourceSampleList.postValue(Resource.loading(null)))
        .subscribeOn(Schedulers.io())
        .subscribe(sampleMasters -> mResourceSampleList.postValue(Resource.success(toViewList(sampleMasters))),
            throwable -> mResourceSampleList.postValue(
                Resource.error(throwable.getMessage(), null))));
  }

  private List<SampleListAdapter2.ItemData> toViewList(List<SampleMaster> sampleMasters) {
    ArrayList<SampleListAdapter2.ItemData> ret = new ArrayList<>();
    for(SampleMaster item:sampleMasters){
      SampleListAdapter2.ItemData itemData = new SampleListAdapter2.ItemData();
      itemData.hold = item;
      itemData.iconPath = item.image1;
      itemData.redFlag = item.isDirty;
      itemData.title = item.desc;
      itemData.timestamp = item.update_date;
      ret.add(itemData);
    }
    return ret;
  }
}
