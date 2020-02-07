package com.ledway.btprinter.biz.main;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.activeandroid.Model;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.reflect.TypeToken;
import com.gturedi.views.StatefulLayout;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.TodoProdDetailActivity;
import com.ledway.btprinter.biz.main.viewmodel.PagingInfo;
import com.ledway.btprinter.event.ProdSaveEvent;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.btprinter.network.model.GroupNameInfo;
import com.ledway.btprinter.network.model.RemoteGroupProduct;
import com.ledway.btprinter.network.model.RestDataSetResponse;
import com.ledway.framework.FullScannerActivity;
import com.ledway.rxbus.RxBus;
import com.ledway.scanmaster.event.ResignedEvent;
import com.ledway.scanmaster.model.Resource;
import com.ledway.scanmaster.utils.BizUtils;
import com.ledway.scanmaster.utils.ContextUtils;
import com.ledway.scanmaster.utils.JsonUtils;
import io.reactivex.disposables.CompositeDisposable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

public class ProductListFragment extends Fragment {
  public static final String DATA_PRODUCTS = "data_products";
  private static final int RESULT_CAMERA_QR_CODE = 1;
  private static final int REQUEST_TODO_PRODUCT = 2;
  @BindView(R.id.listview) RecyclerView mListView;
  @BindView(R.id.swiperefresh) SwipeRefreshLayout mSwipeRefresh;
  @BindView(R.id.statefulLayout) StatefulLayout mStatefulLayout;
  @BindView(R.id.edt_filter) EditText mEdtFilter;
  @BindView(R.id.calc_clear_txt_filter) Button mBtnClear;
  private Unbinder mViewBinder;
  private CompositeDisposable mDisposables = new CompositeDisposable();
  private CompositeSubscription mSubscription = new CompositeSubscription();
  private SampleListAdapter2 mSampleListAdapter;
  private MutableLiveData<Resource<List<SampleListAdapter2.ItemData>>> dataResource =
      new MutableLiveData<>();
  private MutableLiveData<Resource<List<GroupNameInfo>>> showRooms = new MutableLiveData<>();
  private MutableLiveData<Resource> syncProducts = new MutableLiveData<>();
  private ArrayList<String> mSelectedList;
  private ArrayList<SampleListAdapter2.ItemData> mDataList;
  private MutableLiveData<Resource<PagingInfo<List<SampleListAdapter2.ItemData>>>> downloadGroup =
      new MutableLiveData<>();

  private boolean inSelectMode = false;
  private MaterialDialog progress;

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

  protected void checkResignedStatus(){
    if(getActivity() != null){
      SharedPreferences sp = getActivity().getSharedPreferences("setting", Context.MODE_PRIVATE);
      String myTaxNo =sp.getString("MyTaxNo", "");
      String resignedKey = myTaxNo +"_resigned";
      String  resigned = sp.getString(resignedKey, "");
      String  resigned_product = sp.getString(resignedKey +"_product", "");
      if(resigned.equals("Y") && resigned_product.isEmpty()){
        List<Model> tmp =
            new Delete().from(TodoProd.class).where("uploaded_time >= update_time ").execute();
        sp.edit().putString(resignedKey +"_product","Y").commit();
        loadData();
        Toast.makeText(requireContext(),R.string.removed_group_product, Toast.LENGTH_LONG).show();
      }
    }

  }
  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getArguments() != null) {
      inSelectMode = getArguments().getBoolean("select", false);
      mSelectedList = getArguments().getStringArrayList(DATA_PRODUCTS);
    }

    if (mSelectedList == null) {
      mSelectedList = new ArrayList<>();
    }
    mDataList = new ArrayList<>();
    mSampleListAdapter = new SampleListAdapter2(getContext(), mDataList);
    mSampleListAdapter.setSelectMode(inSelectMode);
    mDisposables.add(mSampleListAdapter.getClickObservable()
        .subscribe(prodno -> startActivityForResult(
            new Intent(getActivity(), TodoProdDetailActivity.class).putExtra("prod_no",
                (String) prodno), REQUEST_TODO_PRODUCT)));
    mDisposables.add(mSampleListAdapter.getmLongClickSubject().subscribe(view -> {
      PopupMenu popup = new PopupMenu(getActivity(), (View) view);

      // This activity implements OnMenuItemClickListener
      popup.setOnMenuItemClickListener(menuItem -> {
        if (menuItem.getItemId() == R.id.action_delete) {
          int index = (int) ((View) view).getTag();
          String prodNo = (String) mSampleListAdapter.get(index).hold;
          removeProduct(prodNo);
        } else if (menuItem.getItemId() == R.id.action_delete_all) {
          removeAllProduct();
        }
        return false;
      });
      popup.inflate(R.menu.menu_product_delete);

      popup.show();
    }));

    mDisposables.add(mSampleListAdapter.getCheckObservable().subscribe(pair -> {
      Timber.d("%d", pair.first);
      String prodno = (String) mSampleListAdapter.get(pair.first).hold;
      int index = mSelectedList.indexOf(prodno);
      if (pair.second && index < 0) {
        mSelectedList.add(prodno);
      } else if (!pair.second && index > -1) {
        mSelectedList.remove(prodno);
      }
      titleChange();
    }));
    mSubscription.add(RxBus.getInstance()
        .toObservable(ProdSaveEvent.class)
        .subscribe(prodSaveEvent -> loadData()));

    mSubscription.add(RxBus.getInstance()
        .toObservable(ResignedEvent.class)
        .subscribe(resignedEvent -> checkResignedStatus()));
    checkResignedStatus();
    loadData();
    ((LifecycleRegistry) getLifecycle()).handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    initView();





  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_show_room_list, container, false);
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

    mSubscription.add(RxTextView.textChanges(mEdtFilter)
        .doOnNext(charSequence -> mBtnClear.setVisibility(
            charSequence.length() > 0 ? View.VISIBLE : View.GONE))
        .debounce(800, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(filter -> loadData("%" + filter.toString() + "%")));
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    mViewBinder.unbind();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    mDisposables.clear();
    mSubscription.clear();
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
      case R.id.action_key: {
        new MaterialDialog.Builder(getActivity()).title(R.string.input_product_number)
            .inputType(InputType.TYPE_CLASS_TEXT)
            .input(R.string.input_hint_product_no, 0, (dialog, input) -> {
              if (!TextUtils.isEmpty(input)) {
                receiveQrCode(input.toString());
              }
            })
            .show();
        break;
      }
      case R.id.action_done: {

        getActivity().setResult(Activity.RESULT_OK,
            new Intent().putStringArrayListExtra("selected", mSelectedList));
        getActivity().finish();
        break;
      }
      case R.id.action_download_from_group: {
        loadGroupProduct();
        break;
      }
    }
    return true;
  }

  private void loadGroupProduct() {
    String myTaxNo = BizUtils.getMyTaxNo(getActivity());
    //myTaxNo = "3036A";
    mSubscription.add(MyProjectApi.getInstance()
        .getDbService()
        .customQuery("select * from view_GroupShowName2 where mytaxno ='" + myTaxNo + "'")
        .doOnSubscribe(() -> showRooms.postValue(Resource.loading(null)))
        .map(responseBody -> {
          try {
            String json = responseBody.string();
            Type listType = new TypeToken<RestDataSetResponse<GroupNameInfo>>() {
            }.getType();
            RestDataSetResponse<GroupNameInfo> response =
                JsonUtils.Companion.fromJson(json, listType);
            return response.result.get(0);
          } catch (IOException e) {
            e.printStackTrace();
          }
          return null;
        })
        .subscribeOn(Schedulers.io())
        .subscribe(new Subscriber<ArrayList<GroupNameInfo>>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            showRooms.postValue(Resource.error(ContextUtils.getMessage(e), null));
          }

          @Override public void onNext(ArrayList<GroupNameInfo> groupNameInfos) {
            showRooms.postValue(Resource.success(groupNameInfos));
          }
        }));
  }

  private void scanBarCode() {
    startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
        RESULT_CAMERA_QR_CODE);
  }

  private void loadData() {
    loadData("%");
  }

  private void loadData(String filter) {



    Observable.defer(() ->{
      return Observable.from(new Select().from(TodoProd.class)
        .where("prodno like ? or spec_desc like ?", filter, filter)
        .orderBy("prodno ")
        .execute());}).map(o -> {
      TodoProd todoProd = (TodoProd) o;
      return toViewData(todoProd);
    }).toList().subscribe(new Subscriber<List<SampleListAdapter2.ItemData>>() {
      @Override public void onCompleted() {

      }

      @Override public void onError(Throwable e) {
        dataResource.postValue(Resource.error(ContextUtils.getMessage(e), null));
      }

      @Override public void onNext(List<SampleListAdapter2.ItemData> itemData) {
        dataResource.postValue(Resource.success(itemData));

        if (filter.equals("%")) {
          int i = 0;
          while (i < mSelectedList.size()) {
            boolean found = false;
            for (SampleListAdapter2.ItemData item : itemData) {
              if (item.hold.equals(mSelectedList.get(i))) {
                found = true;
                break;
              }
            }
            if (!found) {
              mSelectedList.remove(i);
            } else {
              ++i;
            }
          }
        }
      }
    });
  }

  private SampleListAdapter2.ItemData toViewData(TodoProd todoProd) {
    SampleListAdapter2.ItemData itemData = new SampleListAdapter2.ItemData();
    itemData.timestamp = todoProd.create_time;
    itemData.subTitle = todoProd.spec_desc;
    itemData.title = todoProd.prodNo;
    itemData.hold = todoProd.prodNo;
    itemData.iconPath = todoProd.image1;
    itemData.redFlag = todoProd.uploaded_time == null
        || todoProd.update_time == null
        || todoProd.update_time.getTime() > todoProd.uploaded_time.getTime();

    itemData.isChecked = mSelectedList.contains(todoProd.prodNo);
    return itemData;
  }

  private void removeProduct(String prodNo) {
    new Delete().from(TodoProd.class).where("prodNo=?", prodNo).execute();
    loadData();
  }

  private void removeAllProduct() {
    new Delete().from(TodoProd.class).execute();
    loadData();
  }

  private void titleChange() {
    ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
    actionbar.setTitle(getString(R.string.formater_selected, mSelectedList.size()));
  }

  @OnClick(R.id.calc_clear_txt_filter) void onClearClick() {
    mEdtFilter.setText("");
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
          //mSampleListAdapter.setData(resource.data);
          mDataList.clear();
          mDataList.addAll(resource.data);
          mSampleListAdapter.notifyDataSetChanged();
          if (resource.data.isEmpty()) {
            mStatefulLayout.showEmpty();
          } else {
            mStatefulLayout.showContent();
          }
        }
      }
    });

    showRooms.observe(this, listResource -> {
      if (listResource == null) return;
      switch (listResource.status) {
        case LOADING: {
          showLoading();
          break;
        }
        case ERROR: {
          hideLoading();
          Toast.makeText(getActivity(), listResource.message, Toast.LENGTH_LONG).show();
          break;
        }
        case SUCCESS: {
          hideLoading();
          List<String> label = Observable.from(listResource.data)
              .map(groupNameInfo -> groupNameInfo.showname
                  + " "
                  + groupNameInfo.ttl
                  + groupNameInfo.showname2)
              .toList()
              .toBlocking()
              .first();
          new MaterialDialog.Builder(getActivity()).title(R.string.exhibition)
              .items(label)
              .alwaysCallSingleChoiceCallback()
              .itemsCallbackSingleChoice(-1, (dialog, view, which, text) -> {
                dialog.setSelectedIndex(which);
                // syncProduct(text.toString());
                GroupNameInfo selected = listResource.data.get(which);
                downloadGroup.postValue(Resource.loading(new PagingInfo<>(selected.ttl, 0, null)));
                downloadGroupProduct(selected.showname, selected.ttl, 0, 1);
                return false;
              })
              .show();
          break;
        }
      }
      showRooms.postValue(null);
    });

    syncProducts.observe(this, resource -> {
      Timber.d("syncProducts");
      switch (resource.status) {
        case LOADING: {
          showLoading();
          break;
        }
        case ERROR: {
          Toast.makeText(getActivity(), resource.message, Toast.LENGTH_LONG).show();
          hideLoading();
          break;
        }
        case SUCCESS: {
          hideLoading();
          loadData();
          break;
        }
      }
    });
    downloadGroup.observe(this, resource -> {
      switch (resource.status) {
        case LOADING: {
          showProgress(resource.data.max, resource.data.position);
          break;
        }
        case ERROR: {
          Toast.makeText(getActivity(), resource.message, Toast.LENGTH_LONG).show();
          hideLoading();
          break;
        }
        case PROGRESSING: {
          showProgress(resource.data.max, resource.data.position);
          for (SampleListAdapter2.ItemData item : resource.data.data) {
            for (int i = 0; i < mDataList.size(); ++i) {
              if (mDataList.get(i).hold.equals(item.hold)) {
                mDataList.remove(i);
                break;
              }
            }
          }
          mDataList.addAll(resource.data.data);
          mSampleListAdapter.notifyDataSetChanged();
          if (mDataList.isEmpty()) {
            mStatefulLayout.showEmpty();
          } else {
            mStatefulLayout.showContent();
          }
          break;
        }
        case SUCCESS: {
          hideLoading();
          break;
        }
      }
    });
  }

  private void showProgress(int max, int position) {
    if (progress == null || progress.isIndeterminateProgress()) {
      hideLoading();
      progress =
          new MaterialDialog.Builder(getActivity()).cancelable(false).progress(false, max).show();
    }
    progress.setProgress(position);
  }

  private void showLoading() {
    hideLoading();
    progress =
        new MaterialDialog.Builder(getActivity()).cancelable(false).progress(true, 100).show();
  }

  private void hideLoading() {
    if (progress != null) {
      progress.dismiss();
      progress = null;
    }
  }

  private void downloadGroupProduct(String room, int totalCount, int offset, int size) {
    String myTaxNo = BizUtils.getMyTaxNo(getActivity());
    mSubscription.add(MyProjectApi.getInstance()
        .getDbService()
        .customQuery("select * from view_GroupShowList where mytaxno ='"
            + myTaxNo
            + "' and showname ='"
            + room
            + "' order by prodno OFFSET "
            + offset
            + " ROWS FETCH NEXT "
            + size
            + " ROWS ONLY")
        .map(responseBody -> {
          try {
            String json = responseBody.string();
            Type listType = new TypeToken<RestDataSetResponse<RemoteGroupProduct>>() {
            }.getType();
            RestDataSetResponse<RemoteGroupProduct> response =
                JsonUtils.Companion.fromJson(json, listType);
            return response.result.get(0);
          } catch (IOException e) {
            e.printStackTrace();
          }
          return null;
        })
        .flatMap(Observable::from)
        .map(remoteGroupProduct -> {
          new Delete().from(TodoProd.class).where("prodNo =?", remoteGroupProduct.prodno).execute();
          TodoProd todoProd = new TodoProd();
          todoProd.prodNo = remoteGroupProduct.prodno;
          todoProd.spec_desc = remoteGroupProduct.specdesc;
          todoProd.update_time =
              remoteGroupProduct.updateDate == null ? new Date() : remoteGroupProduct.updateDate;
          todoProd.uploaded_time = new Date();
          todoProd.create_time =
              remoteGroupProduct.updateDate == null ? new Date() : remoteGroupProduct.updateDate;
          String file1Path =
              MApp.getApplication().getPicPath() + "/product_" + todoProd.prodNo + "_type1.jpg";
          String file2Path =
              MApp.getApplication().getPicPath() + "/product_" + todoProd.prodNo + "_type2.jpg";
          if (!TextUtils.isEmpty(remoteGroupProduct.graphic)) {
            byte[] data = Base64.decode(remoteGroupProduct.graphic, Base64.DEFAULT);
            try (OutputStream stream = new FileOutputStream(new File(file1Path))) {
              stream.write(data);
              todoProd.image1 = file1Path;
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          if (!TextUtils.isEmpty(remoteGroupProduct.graphic2)) {
            byte[] data = Base64.decode(remoteGroupProduct.graphic2, Base64.DEFAULT);
            try (OutputStream stream = new FileOutputStream(new File(file2Path))) {
              stream.write(data);
              todoProd.image2 = file2Path;
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          todoProd.save();
          return toViewData(todoProd);
        })
        .toList()
        //.delay(2, TimeUnit.SECONDS)
        .subscribeOn(Schedulers.io())
        .subscribe(new Subscriber<List<SampleListAdapter2.ItemData>>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            downloadGroup.postValue(Resource.error(ContextUtils.getMessage(e), null));
          }

          @Override public void onNext(List<SampleListAdapter2.ItemData> itemData) {
            if (itemData.isEmpty()) {
              downloadGroup.postValue(Resource.success(null));
            } else {
              downloadGroup.postValue(
                  Resource.progressing(new PagingInfo<>(totalCount, offset + size, itemData)));
              if (totalCount - offset - size > 0) {
                downloadGroupProduct(room, totalCount, offset + size, size);
              } else {
                downloadGroup.postValue(Resource.success(null));
              }
            }
          }
        }));
  }

  private void syncProduct(String room) {
    String myTaxNo = BizUtils.getMyTaxNo(getActivity());
    // myTaxNo = "3036A";

    MyProjectApi.getInstance()
        .getDbService()
        .customQuery("select * from view_GroupShowList where mytaxno ='"
            + myTaxNo
            + "' and showname ='"
            + room
            + "' order by prodno ")
        .doOnSubscribe(() -> syncProducts.postValue(Resource.loading(null)))
        .map(responseBody -> {
          try {
            String json = responseBody.string();
            Type listType = new TypeToken<RestDataSetResponse<RemoteGroupProduct>>() {
            }.getType();
            RestDataSetResponse<RemoteGroupProduct> response =
                JsonUtils.Companion.fromJson(json, listType);
            return response.result.get(0);
          } catch (IOException e) {
            e.printStackTrace();
          }
          return null;
        })
        .flatMap(Observable::from)
        .doOnNext(remoteGroupProduct -> {
          new Delete().from(TodoProd.class).where("prodNo =?", remoteGroupProduct.prodno).execute();
          TodoProd todoProd = new TodoProd();
          todoProd.prodNo = remoteGroupProduct.prodno;
          todoProd.spec_desc = remoteGroupProduct.specdesc;
          todoProd.update_time =
              remoteGroupProduct.updateDate == null ? new Date() : remoteGroupProduct.updateDate;
          todoProd.uploaded_time = new Date();
          todoProd.create_time =
              remoteGroupProduct.updateDate == null ? new Date() : remoteGroupProduct.updateDate;
          String file1Path =
              MApp.getApplication().getPicPath() + "/product_" + todoProd.prodNo + "_type1.jpg";
          String file2Path =
              MApp.getApplication().getPicPath() + "/product_" + todoProd.prodNo + "_type2.jpg";
          if (!TextUtils.isEmpty(remoteGroupProduct.graphic)) {
            byte[] data = Base64.decode(remoteGroupProduct.graphic, Base64.DEFAULT);
            try (OutputStream stream = new FileOutputStream(new File(file1Path))) {
              stream.write(data);
              todoProd.image1 = file1Path;
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          if (!TextUtils.isEmpty(remoteGroupProduct.graphic2)) {
            byte[] data = Base64.decode(remoteGroupProduct.graphic2, Base64.DEFAULT);
            try (OutputStream stream = new FileOutputStream(new File(file2Path))) {
              stream.write(data);
              todoProd.image2 = file2Path;
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          todoProd.save();
        })
        .subscribeOn(Schedulers.io())
        .subscribe(new Subscriber<RemoteGroupProduct>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            syncProducts.postValue(Resource.error(ContextUtils.getMessage(e), null));
          }

          @Override public void onNext(RemoteGroupProduct remoteGroupProduct) {
            syncProducts.postValue(Resource.success(null));
          }
        });
  }
}
