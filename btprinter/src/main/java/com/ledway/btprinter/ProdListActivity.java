package com.ledway.btprinter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import com.activeandroid.Model;
import com.activeandroid.query.Select;
import com.ledway.btprinter.adapters.TodoProdAdapter;
import com.ledway.btprinter.models.TodoProd;
import java.util.ArrayList;
import java.util.List;

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
  }

  private void setView() {
    LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
    linearLayoutManager.setAutoMeasureEnabled(true);
    recyclerView.setLayoutManager(linearLayoutManager);
    mTodoProdList = getToProds();
    mAdapter = new TodoProdAdapter(mTodoProdList);
    recyclerView.setAdapter(mAdapter);
    recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
      @Override public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
      }

      @Override public void onTouchEvent(RecyclerView rv, MotionEvent e) {

      }

      @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

      }
    });
  }

  private List<TodoProd> getToProds(){
    List<TodoProd> todoProds =  new Select().from(TodoProd.class).where("uploaded_time is null").orderBy("created_time").execute();
    return todoProds;
  }

}
