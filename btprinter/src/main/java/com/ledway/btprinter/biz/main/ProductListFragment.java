package com.ledway.btprinter.biz.main;

import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.afollestad.materialdialogs.MaterialDialog;
import com.gturedi.views.StatefulLayout;
import com.ledway.btprinter.R;
import com.ledway.btprinter.TodoProdDetailActivity;
import com.ledway.scanmaster.model.Resource;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.btprinter.utils.ContextUtils;
import com.ledway.framework.FullScannerActivity;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.Subscriber;

public class ProductListFragment extends Fragment {
  public static final String DATA_PRODUCTS = "data_products";
  private static final int RESULT_CAMERA_QR_CODE = 1;
  private static final int REQUEST_TODO_PRODUCT = 2;
  @BindView(R.id.listview) RecyclerView mListView;
  @BindView(R.id.swiperefresh) SwipeRefreshLayout mSwipeRefresh;
  @BindView(R.id.statefulLayout) StatefulLayout mStatefulLayout;
  private Unbinder mViewBinder;
  private CompositeDisposable mDisposables = new CompositeDisposable();
  private SampleListAdapter2 mSampleListAdapter;
  private MutableLiveData<Resource<List<SampleListAdapter2.ItemData>>> dataResource =
      new MutableLiveData<>();
  private ArrayList<String> mDefaultSelected;

  private boolean inSelectMode = false;

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
          receiveQrCode(qrcode);
        }
        break;
      }
      case REQUEST_TODO_PRODUCT: {
        loadData();
        break;
      }
    }
  }

  private void receiveQrCode(String qrcode) {
    startActivityForResult(
        new Intent(getActivity(), TodoProdDetailActivity.class).putExtra("prod_no", qrcode),
        REQUEST_TODO_PRODUCT);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      inSelectMode = getArguments().getBoolean("select", false);
      mDefaultSelected = getArguments().getStringArrayList(DATA_PRODUCTS);
    }
    if(mDefaultSelected == null){
      mDefaultSelected = new ArrayList<>();
    }
    mSampleListAdapter = new SampleListAdapter2(getContext());
    mSampleListAdapter.setSelectMode(inSelectMode);
    mDisposables.add(mSampleListAdapter.getClickObservable()
        .subscribe(prodno -> startActivityForResult(
            new Intent(getActivity(), TodoProdDetailActivity.class).putExtra("prod_no",
                (String) prodno), REQUEST_TODO_PRODUCT)));
    mDisposables.add(mSampleListAdapter.getmLongClickSubject()
        .subscribe(view -> {
          PopupMenu popup = new PopupMenu(getActivity(), (View)view);

          // This activity implements OnMenuItemClickListener
          popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(MenuItem menuItem) {
              int index = (int) ((View) view).getTag();
              String prodNo = (String) mSampleListAdapter.get(index).hold;
              removeProduct(prodNo);
              return false;
            }
          });
          popup.inflate(R.menu.menu_product_delete);
          popup.show();

        }));


    mDisposables.add(mSampleListAdapter.getCheckObservable().subscribe(o -> titleChange()));
    loadData();
  }

  private void removeProduct(String prodNo) {
    new Delete().from(TodoProd.class).where("prodno=?",prodNo).execute();
    loadData();
  }

  private void titleChange() {
    ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    actionbar.setTitle(
        getString(R.string.formater_selected, mSampleListAdapter.getSelection().length));
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

  @Override public void onDestroy() {
    super.onDestroy();
    mDisposables.clear();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_product_list, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override public void onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.action_done).setVisible(inSelectMode);
    menu.findItem(R.id.app_bar_search).setVisible(false);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_add: {
        scanBarCode();
        break;
      }
      case R.id.action_key:{
        new MaterialDialog.Builder(getActivity())
            .title(R.string.input_product_number)
            .inputType(InputType.TYPE_CLASS_TEXT)
            .input(R.string.input_hint_product_no, 0, (dialog, input) -> {
              if(!TextUtils.isEmpty(input)){
                receiveQrCode(input.toString());
              }
            }).show();
        break;
      }
      case R.id.action_done: {
        ArrayList<String> selected = new ArrayList<>();
        SampleListAdapter2.ItemData[] selectedViewItem = mSampleListAdapter.getSelection();
        for( SampleListAdapter2.ItemData viewItem:selectedViewItem){
          selected.add((String) viewItem.hold);
        }
        getActivity().setResult(Activity.RESULT_OK, new Intent().putStringArrayListExtra("selected",selected));
        getActivity().finish();
        break;
      }
    }
    return true;
  }

  private void scanBarCode() {
    startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
        RESULT_CAMERA_QR_CODE);
  }

  private void loadData() {
    Observable.defer(() -> Observable.from(
        new Select().from(TodoProd.class).orderBy("create_time desc").execute())).map(o -> {
      TodoProd todoProd = (TodoProd) o;
      SampleListAdapter2.ItemData itemData = new SampleListAdapter2.ItemData();
      itemData.timestamp = todoProd.create_time;
      itemData.subTitle = todoProd.spec_desc;
      itemData.title = todoProd.prodNo;
      itemData.hold = todoProd.prodNo;
      itemData.iconPath = todoProd.image1;
      itemData.redFlag = todoProd.uploaded_time == null
          || todoProd.update_time.getTime() > todoProd.uploaded_time.getTime();

      itemData.isChecked = mDefaultSelected.contains(todoProd.prodNo);
      return itemData;
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
}
