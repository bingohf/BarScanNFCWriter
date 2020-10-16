package com.ledway.btprinter.biz.sample;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.gturedi.views.StatefulLayout;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.biz.main.SampleListAdapter2;
import com.ledway.btprinter.models.ReceivedSample;
import com.ledway.btprinter.models.SampleProdLink;
import com.ledway.scanmaster.utils.JsonUtils;
import io.reactivex.disposables.CompositeDisposable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.activeandroid.Cache.getContext;

/**
 * Created by togb on 2017/12/10.
 */

public class ReceivedSampleDetailActivity extends AppCompatActivity {
  @BindView(R.id.listview) RecyclerView mListView;
  @BindView(R.id.swiperefresh) SwipeRefreshLayout mSwipeRefresh;
  @BindView(R.id.statefulLayout) StatefulLayout mStatefulLayout;
  private SampleListAdapter2 mSampleListAdapter;
  private SampleProdLink[] mList;
  private CompositeDisposable mDisposables = new CompositeDisposable();

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.fragment_sample_list);
    ButterKnife.bind(this);

    mSampleListAdapter = new SampleListAdapter2(this);

    String guid = getIntent().getStringExtra("guid");
    try {
      loadData(guid);
    } catch (IOException e) {
      e.printStackTrace();
    }

    LinearLayoutManager layoutManager =
        new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    mListView.setLayoutManager(layoutManager);
    mListView.setAdapter(mSampleListAdapter);
    DividerItemDecoration dividerItemDecoration =
        new DividerItemDecoration(this, layoutManager.getOrientation());
    mListView.addItemDecoration(dividerItemDecoration);
    mListView.setAdapter(mSampleListAdapter);

    mListView.setVisibility(View.VISIBLE);
    mSwipeRefresh.setEnabled(false);
    mDisposables.add(mSampleListAdapter.getClickObservable().subscribe(hold -> {
      File imageFile = new File((String)hold);
      if(imageFile.exists()){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(
            FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider",
                imageFile), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getContext().startActivity(intent);
      }

    }));
    showData();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mDisposables.clear();
  }

  private void showData() {
    if (mList.length > 0) {

      ArrayList<SampleListAdapter2.ItemData> viewList = new ArrayList<>();
      for (SampleProdLink item : mList) {
        viewList.add(toViewItem(item));
      }
      mSampleListAdapter.setData(viewList);
      mSampleListAdapter.notifyDataSetChanged();
      mStatefulLayout.showContent();
    }
  }

  private SampleListAdapter2.ItemData toViewItem(SampleProdLink link) {
    SampleListAdapter2.ItemData itemData = new SampleListAdapter2.ItemData();
    // itemData.timestamp = todoProd.create_time;
    itemData.subTitle = link.spec_desc;
    itemData.title = link.prod_id;
    itemData.iconPath = MApp.getApplication().getPicPath() + "/product/" + link.prod_id + ".png";
    itemData.hold = itemData.iconPath ;
    itemData.redFlag = false;
    return itemData;
  }

  private void loadData(String guid) throws IOException {
    List<Model> list = new Select().from(ReceivedSample.class).where("hold_id =?", guid).execute();
    mList = new SampleProdLink[0];
    if (list.isEmpty()) {
      Toast.makeText(this, R.string.not_found_data, Toast.LENGTH_LONG).show();
    } else {

      ReceivedSample item = (ReceivedSample) list.get(0);
      mList = JsonUtils.Companion.fromJson(item.detailJson, SampleProdLink[].class);
    }
  }
}
