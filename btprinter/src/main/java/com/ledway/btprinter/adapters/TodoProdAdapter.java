package com.ledway.btprinter.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.ledway.btprinter.R;
import com.ledway.btprinter.models.TodoProd;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by togb on 2016/7/3.
 */
public class TodoProdAdapter extends RecyclerView.Adapter<ToProdViewHolder> {
  private List<TodoProd> todoProds;
  public TodoProdAdapter(List<TodoProd> todoProds){
    this.todoProds = todoProds;
  }


  @Override public ToProdViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    View view = layoutInflater.inflate(R.layout.list_item_todo_prod, parent, false);
    return new ToProdViewHolder(view);
  }

  @Override public void onBindViewHolder(ToProdViewHolder holder, int position) {
    TodoProd todoProd = todoProds.get(position);
    holder.textView.setText(todoProd.prodNo);
  }

  @Override public int getItemCount() {
    return todoProds.size();
  }


}
