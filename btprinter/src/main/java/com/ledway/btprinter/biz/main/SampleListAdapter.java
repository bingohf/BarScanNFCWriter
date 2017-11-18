package com.ledway.btprinter.biz.main;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.ledway.btprinter.R;

public class SampleListAdapter  extends RecyclerView.Adapter<SampleListAdapter.SampleViewHolder>{


  private final LayoutInflater mLayoutInflater;

  public SampleListAdapter(Context context) {
    mLayoutInflater = LayoutInflater.from(context);
  }

  @Override public SampleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = mLayoutInflater.inflate(R.layout.list_item_sample, parent, false);
    return new SampleViewHolder(view);
  }

  @Override public void onBindViewHolder(SampleViewHolder holder, int position) {

  }

  @Override public int getItemCount() {
    return 10;
  }

  public static class SampleViewHolder extends RecyclerView.ViewHolder{

    public SampleViewHolder(View itemView) {
      super(itemView);
    }
  }
}
