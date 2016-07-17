package com.ledway.btprinter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.ledway.btprinter.adapters.TodoProdAdapter;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.framework.RemoteDB;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/7/3.
 */
public class ProdListActivity extends AppCompatActivity {

  private RecyclerView recyclerView;
  private List<TodoProd> mTodoProdList;
  private TodoProdAdapter mAdapter;

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

  private List<TodoProd> getToProds(){
    List<TodoProd> todoProds =  new Select(new String[]{"id", "prodno","uploaded_time","spec_desc"}).from(TodoProd.class).orderBy("uploaded_time").execute();
    return todoProds;
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
    String connectionString =
        "jdbc:jtds:sqlserver://vip.ledway.com.tw:1433;DatabaseName=iSamplePub;charset=UTF8";
    final RemoteDB remoteDB = new RemoteDB(connectionString);
    remoteDB.executeQuery("select count(distinct salesno) total_Users from dbo.PRODUCTAPPGET")
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<ResultSet>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            int count = sp.getInt("count",0);
            if (count >0){
              getSupportActionBar().setTitle("產品  總用戶數:" + count);
            }
          }

          @Override public void onNext(ResultSet resultSet) {
            try {
              while(resultSet.next()) {
                int count = resultSet.getInt("total_Users");
                if (count > 0) {
                  getSupportActionBar().setTitle("產品  總用戶數:" + count);
                }
                sp.edit()
                    .putInt("count", count)
                    .apply();
              }
            } catch (SQLException e) {
              e.printStackTrace();
            }
          }
        });
  }
}
