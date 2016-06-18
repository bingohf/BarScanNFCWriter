package com.ledway.btprinter.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.ledway.btprinter.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by togb on 2016/6/4.
 */
public class DataAdapter extends RecyclerView.Adapter<BaseViewHolder>{
  public final static int DATA_TYPE_MEMO =      0;
  public final static int DATA_TYPE_PHOTO_1 =  1;
  public final static int DATA_TYPE_PHOTO_2 =  2;
  public final static int DATA_TYPE_BARCODE =  3;
  public final static int DATA_TYPE_QR_CODE =  4;

  private Context context;
  private ArrayList<BaseData> mData = new ArrayList<>();

  public DataAdapter(Context context) {
    this.context = context;

  }

  @Override public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    BaseViewHolder viewHolder = null;
    switch (viewType){
      case DATA_TYPE_BARCODE:
      case DATA_TYPE_MEMO:{
        View view = inflater.inflate(R.layout.list_item_text, parent, false);
        return new TextViewHolder(view);
      }
      case DATA_TYPE_QR_CODE:
      case DATA_TYPE_PHOTO_1:
      case DATA_TYPE_PHOTO_2:{
        View view = inflater.inflate(R.layout.list_item_photo, parent, false);
        return new PhotoViewHolder(view);
      }
    }
    return null;
  }

  @Override public void onBindViewHolder(BaseViewHolder holder, int position) {
    holder.changeData(mData.get(position));
  }

  @Override public int getItemViewType(int position) {
    return mData.get(position).getType();
  }

  @Override public int getItemCount() {
    return mData.size();
  }

  public void addData(BaseData data){
     mData.add(data);
    Collections.sort(mData, new Comparator<BaseData>() {
      @Override public int compare(BaseData lhs, BaseData rhs) {
        return lhs.getType() - rhs.getType();
      }
    });
    notifyDataSetChanged();
  }

}
