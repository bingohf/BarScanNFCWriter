package com.ledway.btprinter.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.activeandroid.query.Select;
import com.ledway.btprinter.AppConstants;
import com.ledway.btprinter.BusinessCardActivity;
import com.ledway.btprinter.ItemDetailActivity;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.ProdListActivity;
import com.ledway.btprinter.R;
import com.ledway.btprinter.adapters.RecordAdapter;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.framework.RemoteDB;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by togb on 2016/9/4.
 */
public class MainFragment extends PagerFragment {

  private RemoteDB remoteDB;
  private PublishSubject<Boolean> mSettingSubject = PublishSubject.create();
  private RecordAdapter mRecordAdapter;
  private CompositeSubscription mSubscriptions = new CompositeSubscription();

  public MainFragment() {
    setHasOptionsMenu(true);
  }

  @Override public String getTitle() {
    return MApp.getApplication().getString(R.string.my_sample);
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_main_list, container, false);
    initView(view);
    return view;
  }

  private void initView(View view) {
    ListView listView = (ListView) view.findViewById(R.id.list_record);
    mRecordAdapter = new RecordAdapter(getActivity());
    listView.setAdapter(mRecordAdapter);
    RecordAdapter.setSingletonInstance(mRecordAdapter);
    getRecordData();
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SampleMaster sampleMaster = mRecordAdapter.getItem(position);
        MApp.getApplication().getSession().put("current_data", sampleMaster);
        startActivityForResult(new Intent(getActivity(), ItemDetailActivity.class), AppConstants.REQUEST_TYPE_MODIFY_RECORD);
      }
    });

    getActivity().findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        SampleMaster sampleMaster = new SampleMaster();
        MApp.getApplication().getSession().put("current_data", sampleMaster);
        startActivityForResult(new Intent(getActivity(), ItemDetailActivity.class),
            AppConstants.REQUEST_TYPE_ADD_RECORD);
      }
    });

  }

  private void getRecordData() {
    List<SampleMaster> dataList = new Select(new String[] {
        "create_date", "desc", "update_date", "guid", "id", "mac_address", "isDirty", "line",
        "reader", "qrcode"
    }).from(SampleMaster.class).where("dataFrom = ''").orderBy(" update_date desc ").execute();
    mRecordAdapter.clear();
    for (SampleMaster sampleMaster : dataList) {
      mRecordAdapter.addData(sampleMaster);
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    RecordAdapter.setSingletonInstance(null);
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    SampleMaster currentData =
        (SampleMaster) MApp.getApplication().getSession().getValue("current_data");
    switch (requestCode) {
      case AppConstants.REQUEST_TYPE_ADD_RECORD: {
        if (resultCode != -1) {
          mRecordAdapter.addData(0, currentData);
        }
        break;
      }
      case AppConstants.REQUEST_TYPE_MODIFY_RECORD: {
        if (resultCode != -1) {
          mRecordAdapter.moveToTop(currentData);
        }
        break;
      }
    }
    mRecordAdapter.notifyDataSetChanged();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.main_fragment_menu, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_upload: {
        uploadAll();
        return true;
      }
    }
    return false;
  }

  private void uploadAll() {
    final ProgressDialog progressDialog =
        ProgressDialog.show(getActivity(), getString(R.string.upload), getString(R.string.wait_a_moment),
            false);
    Observable.from(mRecordAdapter)
        .filter(new Func1<SampleMaster, Boolean>() {
          @Override public Boolean call(SampleMaster sampleMaster) {
            return !sampleMaster.isUploaded();
          }
        })
        .flatMap(new Func1<SampleMaster, Observable<SampleMaster>>() {
          @Override public Observable<SampleMaster> call(SampleMaster sampleMaster) {
            return sampleMaster.remoteSave();
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<SampleMaster>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
            mRecordAdapter.notifyDataSetChanged();
          }

          @Override public void onError(Throwable e) {
            progressDialog.dismiss();
            Log.e("upload_all", e.getMessage(), e);
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
          }

          @Override public void onNext(SampleMaster sampleMaster) {

          }
        });
  }
}
