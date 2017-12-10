package com.ledway.btprinter.biz.sample;

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
import android.text.TextUtils;
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
import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.gturedi.views.StatefulLayout;
import com.ledway.btprinter.R;
import com.ledway.btprinter.biz.ProductPickerActivity;
import com.ledway.btprinter.biz.main.ProductListFragment;
import com.ledway.btprinter.biz.main.SampleListAdapter2;
import com.ledway.btprinter.models.Resource;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.framework.FullScannerActivity;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import rx.Observable;
import rx.Subscriber;

public class SampleProductListFragment extends Fragment {
  private static final int REQUEST_PICKER = 1;
  private static final int RESULT_CAMERA_QR_CODE = 2;
  @BindView(R.id.listview) RecyclerView mListView;
  @BindView(R.id.swiperefresh) SwipeRefreshLayout mSwipeRefresh;
  @BindView(R.id.statefulLayout) StatefulLayout mStatefulLayout;
  private Unbinder mViewBinder;
  private SampleListAdapter2 mSampleListAdapter;
  private List<SampleListAdapter2.ItemData> viewData = new ArrayList<>();
  private MutableLiveData<Resource<List<SampleListAdapter2.ItemData>>> dataResource =
      new MutableLiveData<>();
  private SampleMaster mSampleMaster;

  public SampleProductListFragment() {
    setHasOptionsMenu(true);
  }



  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_PICKER: {
        if (resultCode == Activity.RESULT_OK) {
          receiveSelected(data.getStringArrayListExtra("selected"));
        }
        break;
      }
      case RESULT_CAMERA_QR_CODE: {
        if (resultCode == Activity.RESULT_OK) {
          String qrcode = data.getStringExtra("barcode");
          appendOrNewProduct(qrcode);
        }
        break;
      }
    }
  }

  private void appendOrNewProduct(String qrcode) {
    for (int i =0;i < viewData.size() ; ++i){
      if(viewData.get(i).hold.equals(qrcode)){
        Toast.makeText(getContext(),R.string.prod_exists, Toast.LENGTH_LONG).show();
        return;
      }
    }
    List<Model> list = new Select().from(TodoProd.class).where("prodno = ?", qrcode).execute();
    if(list.isEmpty()){

    } else {
      viewData.add(0, toViewItem((TodoProd) list.get(0)));
      mSampleListAdapter.setData(viewData);
      mSampleListAdapter.notifyDataSetChanged();
      add((TodoProd) list.get(0));
    }
  }

  private void add(TodoProd todoProd){
    SampleProdLink prodLink = new SampleProdLink();
    prodLink.prod_id = todoProd.prodNo;
    prodLink.create_date = new Date();
    prodLink.sample_id = mSampleMaster.guid;
    prodLink.ext = mSampleMaster.sampleProdLinks.size();
    prodLink.link_id = mSampleMaster.guid + "_"+ prodLink.ext;
    prodLink.spec_desc = todoProd.spec_desc;
    mSampleMaster.sampleProdLinks.add(prodLink);
  }

  private void receiveSelected(ArrayList<String> selected) {
    Character[] placeholderArray = new Character[selected.size()];
    String[] paramArray = new String[selected.size()];
    for(int i =0;i< selected.size(); ++i){
      placeholderArray[i] ='?';
      paramArray[i] = selected.get(i);
    }
    while (mSampleMaster.sampleProdLinks.size()>0){
      SampleProdLink link = mSampleMaster.sampleProdLinks.get(0);
      if(link.getId() != null) {
        link.delete();
      }
      mSampleMaster.sampleProdLinks.remove(0);
    }
    Observable.defer(() -> Observable.from(new Select().from(TodoProd.class)
        .where("prodno in (" + TextUtils.join(",", placeholderArray) + ")", paramArray)
        .orderBy("update_time desc")
        .execute())).map(o -> {
      TodoProd todoProd = (TodoProd) o;
      add(todoProd);
      return toViewItem(todoProd);
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

  private SampleListAdapter2.ItemData toViewItem(TodoProd todoProd){
    SampleListAdapter2.ItemData itemData = new SampleListAdapter2.ItemData();
   // itemData.timestamp = todoProd.create_time;
    itemData.subTitle = todoProd.spec_desc;
    itemData.title = todoProd.prodNo;
    itemData.hold = todoProd.prodNo;
    itemData.iconPath = todoProd.image1;
    itemData.redFlag = todoProd.uploaded_time == null
        || todoProd.update_time.getTime() > todoProd.uploaded_time.getTime();
    return itemData;
  }


  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    mSampleMaster =
        getActivity() != null ? ((SampleActivity) getActivity()).mSampleMaster
            : null;
    super.onCreate(savedInstanceState);
    ArrayList<String> prodList = new ArrayList<>();
    for ( SampleProdLink item:mSampleMaster.sampleProdLinks){
      prodList.add(item.prod_id);
    }
    receiveSelected(prodList);
    mSampleListAdapter = new SampleListAdapter2(getContext());
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_sample_list, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    mViewBinder = ButterKnife.bind(this, view);
    LinearLayoutManager layoutManager =
        new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    mListView.setLayoutManager(layoutManager);
    mListView.setAdapter(mSampleListAdapter);
    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(view.getContext(), layoutManager.getOrientation());
    mListView.addItemDecoration(dividerItemDecoration);
    mSwipeRefresh.setEnabled(false);
    initView();
    super.onViewCreated(view, savedInstanceState);
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
          viewData = resource.data;
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
    inflater.inflate(R.menu.menu_sample_product, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_add: {
        ArrayList<String> selected = new ArrayList<>();
        for(int i =0;i < viewData.size(); ++i){
          selected.add((String)viewData.get(i).hold);
        }

        startActivityForResult(new Intent(getActivity(), ProductPickerActivity.class).putExtra(
            ProductListFragment.DATA_PRODUCTS, selected), REQUEST_PICKER);
        break;
      }
      case R.id.action_scan:{

        startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
            RESULT_CAMERA_QR_CODE);
        break;
      }
      default:
        return false;
    }
    return true;
  }
}
