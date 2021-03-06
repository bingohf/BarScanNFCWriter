package com.ledway.btprinter.adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ledway.btprinter.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Created by togb on 2016/6/4.
 */
public class DataAdapter extends RecyclerView.Adapter<BaseViewHolder> implements Iterable<BaseData>{
  public final static int DATA_TYPE_LOGO =      -1;
  public final static int DATA_TYPE_DATA_FROM =      0;
  public final static int DATA_TYPE_PHOTO_1 =  1;
  public final static int DATA_TYPE_PHOTO_2 =  2;
  public final static int DATA_TYPE_MEMO =      3;
  public final static int DATA_TYPE_SHARE_TO =      4;
  public final static int DATA_TYPE_BARCODE =  5;
  public final static int DATA_TYPE_QR_CODE =  6;


  private Context context;
  private ArrayList<BaseData> mData = new ArrayList<>();

  public DataAdapter(Context context) {
    this.context = context;

  }

  @Override public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    BaseViewHolder viewHolder = null;
    switch (viewType){
      case DATA_TYPE_DATA_FROM:
      case DATA_TYPE_SHARE_TO:
      case DATA_TYPE_BARCODE:
      case DATA_TYPE_MEMO:{
        View view = inflater.inflate(R.layout.list_item_text, parent, false);
        return new TextViewHolder(view);
      }
      case DATA_TYPE_LOGO:
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
  public void clear(){
    mData.clear();
    notifyDataSetChanged();
  }

  public void removeByType(int type){
    for (int i =0 ;i < mData.size();++i){
      if (mData.get(i).getType() == type){
        mData.remove(i);
      }
    }
  }

  public BaseData getItem(int position){
    return mData.get(position);
  }

  @Override public Iterator<BaseData> iterator() {
    return mData.iterator();
  }
}
