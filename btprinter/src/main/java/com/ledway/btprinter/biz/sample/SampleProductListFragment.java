package com.ledway.btprinter.biz.sample;

import android.app.Activity;
import androidx.lifecycle.MutableLiveData;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
import com.ledway.btprinter.TodoProdDetailActivity;
import com.ledway.btprinter.biz.ProductPickerActivity;
import com.ledway.btprinter.biz.main.ProductListFragment;
import com.ledway.btprinter.biz.main.SampleListAdapter2;
import com.ledway.scanmaster.model.Resource;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.scanmaster.utils.ContextUtils;
import com.ledway.framework.FullScannerActivity;
import com.ledway.scanmaster.utils.JsonUtils;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import rx.Observable;
import rx.Subscriber;

public class SampleProductListFragment extends Fragment {
  private static final int REQUEST_PICKER = 1;
  private static final int RESULT_CAMERA_QR_CODE = 2;
  private static final int REQUEST_PRODUCT = 3;
  private static final int RESULT_CAMERA_QR_CODE2 = 4;
  @BindView(R.id.listview) RecyclerView mListView;
  @BindView(R.id.swiperefresh) SwipeRefreshLayout mSwipeRefresh;
  @BindView(R.id.statefulLayout) StatefulLayout mStatefulLayout;
  private Unbinder mViewBinder;
  private SampleListAdapter2 mSampleListAdapter;
  private List<SampleListAdapter2.ItemData> viewData = new ArrayList<>();
  private MutableLiveData<Resource<List<SampleListAdapter2.ItemData>>> dataResource =
      new MutableLiveData<>();
  private SampleMaster mSampleMaster;
  private CompositeDisposable mDisposables = new CompositeDisposable();

  public SampleProductListFragment() {
    setHasOptionsMenu(true);
  }



  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_PICKER: {
        if (resultCode == Activity.RESULT_OK) {
          receiveSelected(data.getStringArrayListExtra("selected"));
          mSampleMaster.isDirty = true;
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
      case RESULT_CAMERA_QR_CODE2: {
        if (resultCode == Activity.RESULT_OK) {
          String qrcode = data.getStringExtra("barcode");
          appendOrNewProduct(qrcode);
          startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
              RESULT_CAMERA_QR_CODE2);
        }
        break;
      }
      case REQUEST_PRODUCT:{
        String prodJson = data.getStringExtra("prodJson");
        if(!TextUtils.isEmpty(prodJson)){
          SampleProdLink prodLink = JsonUtils.Companion.fromJson(prodJson, SampleProdLink.class);
          if (TextUtils.isEmpty(prodLink.prod_id)){
            prodLink.prod_id = prodLink.prodNo;
          }
          int index = mSampleMaster.sampleProdLinks.size();
          for (int i =0;i < mSampleMaster.sampleProdLinks.size(); ++i){
            if(mSampleMaster.sampleProdLinks.get(i).prodNo.equals(prodLink.prodNo)){
              index = i;
              mSampleMaster.sampleProdLinks.remove(i);
              break;
            }
          }
          mSampleMaster.sampleProdLinks.add(index ,prodLink);
          Observable.from(mSampleMaster.sampleProdLinks ).map(this::toViewItem).toList().subscribe(
              itemData -> dataResource.postValue(Resource.success(itemData)));
        }


        break;
      }
    }
  }

  private void appendOrNewProduct(String qrcode) {
    for (int i =0;i < viewData.size() ; ++i){
      String json = (String) viewData.get(i).hold;
      SampleProdLink link = JsonUtils.Companion.fromJson(json, SampleProdLink.class);
      if(link.prodNo.equals(qrcode)){
        Toast.makeText(getContext(),R.string.prod_exists, Toast.LENGTH_LONG).show();
        return;
      }
    }
    List<Model> list = new Select().from(TodoProd.class).where("prodNo = ?", qrcode).execute();
    if(list.isEmpty()){

    } else {
      viewData.add(0, toViewItem((TodoProd) list.get(0)));
      mSampleListAdapter.setData(viewData);
      mSampleListAdapter.notifyDataSetChanged();
      mStatefulLayout.showContent();
      add((TodoProd) list.get(0));
      mSampleMaster.isDirty = true;
      Toast.makeText(requireContext(), R.string.success_add, Toast.LENGTH_SHORT).show();
    }
  }

  private void add(TodoProd todoProd){
    SampleProdLink prodLink = new SampleProdLink();
    prodLink.prodNo = todoProd.prodNo;
    prodLink.create_time = new Date();
    prodLink.update_time = todoProd.update_time;
    prodLink.uploaded_time = todoProd.uploaded_time;
    prodLink.image1 = todoProd.image1;
    prodLink.ext = mSampleMaster.sampleProdLinks.size();
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
      mSampleMaster.sampleProdLinks.remove(0);
    }
    Observable.defer(() -> Observable.from(new Select().from(TodoProd.class)
        .where("prodNo in (" + TextUtils.join(",", placeholderArray) + ")", (Object[]) paramArray)
        .orderBy("update_time desc")
        .execute())).map(o -> {
      TodoProd todoProd = (TodoProd) o;
      add(todoProd);
      return toViewItem(todoProd);
    }).toList().subscribe(new Subscriber<List<SampleListAdapter2.ItemData>>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        dataResource.postValue(Resource.error(ContextUtils.getMessage(e), null));
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
    itemData.hold = JsonUtils.Companion.toJson(todoProd);
    itemData.iconPath = todoProd.image1;
    itemData.redFlag = todoProd.uploaded_time == null
        || todoProd.update_time.getTime() > todoProd.uploaded_time.getTime();
    return itemData;
  }

  private SampleListAdapter2.ItemData toViewItem(SampleProdLink prodLink){
    SampleListAdapter2.ItemData itemData = new SampleListAdapter2.ItemData();
    // itemData.timestamp = todoProd.create_time;
    itemData.subTitle = prodLink.spec_desc;
    itemData.title = prodLink.prodNo;
    itemData.hold = JsonUtils.Companion.toJson(prodLink);
    itemData.iconPath = prodLink.image1;
    itemData.redFlag = prodLink.uploaded_time == null
        || prodLink.update_time.getTime() > prodLink.uploaded_time.getTime();
    return itemData;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    mSampleMaster =
        getActivity() != null ? ((SampleActivity) getActivity()).mSampleMaster
            : null;
    super.onCreate(savedInstanceState);
    mSampleListAdapter = new SampleListAdapter2(getContext());

    Observable.from(mSampleMaster.sampleProdLinks ).map(this::toViewItem).toList().subscribe(
        itemData -> dataResource.postValue(Resource.success(itemData)));

    mDisposables.add(mSampleListAdapter.getClickObservable()
        .subscribe(json -> startActivityForResult(
            new Intent(getActivity(), TodoProdDetailActivity.class).putExtra("prodJson",
      (String) json),REQUEST_PRODUCT)));
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
    mDisposables.clear();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_sample_product, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_add: {
        ArrayList<String> selected = new ArrayList<>();
        for(int i =0;i < viewData.size(); ++i){
          String json = (String) viewData.get(i).hold;
          SampleProdLink link = JsonUtils.Companion.fromJson(json, SampleProdLink.class);
          selected.add(link.prodNo);
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
      case R.id.action_multi_scan:{
        startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
            RESULT_CAMERA_QR_CODE2);
        break;
      }
      default:
        return false;
    }
    return true;
  }
}
