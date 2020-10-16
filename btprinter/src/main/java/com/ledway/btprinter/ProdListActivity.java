package com.ledway.btprinter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.activeandroid.Cache;
import com.activeandroid.TableInfo;
import com.activeandroid.util.Log;
import com.ledway.btprinter.adapters.TodoProdAdapter;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.btprinter.network.model.RestDataSetResponse;
import com.ledway.btprinter.network.model.TotalUserReturn;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/7/3.
 */
public class ProdListActivity extends AppCompatActivity {
  private static final int MODE_ALL = 0;
  private static final int MODE_TODAY = 1;
  private RecyclerView recyclerView;
  private List<TodoProd> mTodoProdList;
  private TodoProdAdapter mAdapter;
  private List<TodoProd> mTempProdList = new ArrayList<>();
  private int mMode = MODE_ALL;
  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_prod_list);
    recyclerView = (RecyclerView) findViewById(R.id.list_view);
    setView();
    setRecordCount();
  }

  @Override protected void onResume() {
    super.onResume();
  }

  private void setView() {
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    recyclerView.setLayoutManager(linearLayoutManager);
    mTodoProdList = getToProds();
    mAdapter = new TodoProdAdapter(this, mTodoProdList);
    recyclerView.setAdapter(mAdapter);
    recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
      @Override public void onItemClick(View view, int position) {
        TodoProd todoProd = mTodoProdList.get(position);
        if (todoProd != null){
          MApp.getApplication().getSession().put("current_todo_prod", todoProd);
          startActivityForResult(new Intent(ProdListActivity.this, TodoProdDetailActivity.class),0);
        }
      }
    }));
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.prod_list_menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.action_today:{
        toggleMode(item);
        break;
      }
    }
    return true;
  }

  private void toggleMode(MenuItem trigger){
    switch (mMode){
      case MODE_ALL:{
        mTempProdList.clear();
        for(TodoProd prod: mTodoProdList){
          mTempProdList.add(prod);
        }
        for(int i =0;i < mTodoProdList.size() ;){
          if(mTodoProdList.get(i).todayCount < 1){
            mTodoProdList.remove(i);
          }else{
            ++i;
          }
        }

        mMode = MODE_TODAY;
        break;
      }
      case MODE_TODAY:{
        mTodoProdList.clear();
        for(TodoProd prod: mTempProdList){
          mTodoProdList.add(prod);
        }
        mMode = MODE_ALL;
        break;
      }
    }
    mAdapter.notifyDataSetChanged();
    invalidateOptionsMenu();
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.action_today).setTitle(mMode == MODE_TODAY ?"All":"Today");
    return true;
  }

  private List<TodoProd> getToProds(){

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.set(Calendar.HOUR,0);
    calendar.set(Calendar.MINUTE,0);
    calendar.set(Calendar.MILLISECOND,0);
    calendar.set(Calendar.SECOND,0);
    Cursor cursor = Cache.openDatabase().rawQuery("select todo_prod.*,a.totalCount,b.todayCount from todo_prod "
        + "left join (select count(*) totalCount, prod_id from SampleProdLink group by prod_id) a on a.prod_id = prodno "
        + " left join(select count(*) todayCount ,prod_id from SampleProdLink where create_date >= "+  calendar.getTimeInMillis() +" group by prod_id ) b on b.prod_id = prodno"
        + " order by todo_prod.uploaded_time", new String[]{});
    TableInfo tableInfo = Cache.getTableInfo(TodoProd.class);
    String idName = tableInfo.getIdName();
    final List<TodoProd> entities = new ArrayList<TodoProd>();

    try {
      Constructor<?> entityConstructor = TodoProd.class.getConstructor();

      if (cursor.moveToFirst()) {
        /**
         * Obtain the columns ordered to fix issue #106 (https://github.com/pardom/ActiveAndroid/issues/106)
         * when the cursor have multiple columns with same name obtained from join tables.
         */
        List<String> columnsOrdered = new ArrayList<String>(Arrays.asList(cursor.getColumnNames()));
        do {
          TodoProd entity =
              (TodoProd) Cache.getEntity(TodoProd.class, cursor.getLong(columnsOrdered.indexOf(idName)));
          if (entity == null) {
            entity = (TodoProd) entityConstructor.newInstance();
          }
          entity.loadFromCursor(cursor);
          entity.totalCount = cursor.getInt(cursor.getColumnIndex("totalCount"));
          entity.todayCount = cursor.getInt(cursor.getColumnIndex("todayCount"));
          entities.add((TodoProd) entity);
        }
        while (cursor.moveToNext());
      }

    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException(
          "Your model "  + " does not define a default " +
              "constructor. The default constructor is required for " +
              "now in ActiveAndroid models, as the process to " +
              "populate the ORM model is : " +
              "1. instantiate default model " +
              "2. populate fields"
      );
    }
    catch (Exception e) {
      Log.e("Failed to process cursor.", e);
    }

    return entities;

  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    mAdapter.notifyDataSetChanged();
  }
  private void setRecordCount() {
    final SharedPreferences sp = getSharedPreferences("record_count", Context.MODE_PRIVATE);
    int count = sp.getInt("count",0);
    if (count >0){
      getSupportActionBar().setTitle("產品  總用戶數:" + count);
    }

    MyProjectApi.getInstance().getDbService().queryTotalUser("select count(distinct salesno) total_Users from dbo.PRODUCTAPPGET")
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<RestDataSetResponse<TotalUserReturn>>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            int count = sp.getInt("count",0);
            if (count >0){
              getSupportActionBar().setTitle("產品  總用戶數:" + count);
            }
          }

          @Override
          public void onNext(RestDataSetResponse<TotalUserReturn> response) {
              int count = response.result.get(0).get(0).total_users;
              if (count > 0) {
                getSupportActionBar().setTitle("產品  總用戶數:" + count);
              }
              sp.edit()
                  .putInt("count", count)
                  .apply();
          }
        });

  }
}
