package com.ledway.btprinter.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.ledway.btprinter.R;
import com.ledway.btprinter.models.SampleMaster;
import java.util.ArrayList;
import java.lang.Iterable;
import java.util.Iterator;
import org.w3c.dom.Text;

/**
 * Created by togb on 2016/6/5.
 */
public class RecordAdapter extends BaseAdapter implements Iterable<SampleMaster>{

  private final LayoutInflater mInflater;
  private ArrayList<SampleMaster> mDataList = new ArrayList<>();
  private Context mContext;
  public RecordAdapter(Context context){
    mContext = context;
    mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }
  @Override public int getCount() {
    return mDataList.size();
  }

  @Override public SampleMaster getItem(int position) {
    return mDataList.get(position);
  }

  @Override public long getItemId(int position) {
    return position;
  }

  @Override public View getView(int position, View convertView, ViewGroup parent) {
    Context context = parent.getContext();
    ViewHolder holder = null;
    if (convertView == null) {
      convertView = mInflater.inflate(R.layout.list_item_record, null);
      holder = new ViewHolder();
      holder.textView = (TextView)convertView.findViewById(R.id.text1);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder)convertView.getTag();
    }
    SampleMaster sampleMaster = mDataList.get(position);
    String text = sampleMaster.create_date.toLocaleString()+ (TextUtils.isEmpty(sampleMaster.desc)?"": "\r\n" +sampleMaster.desc);
    if (TextUtils.isEmpty(sampleMaster.qrcode) ){
      text = " * " + text;
    }
    holder.textView.setText(text);
    return convertView;
  }

  @Override public Iterator<SampleMaster> iterator() {
    return mDataList.iterator();
  }

  private static class ViewHolder{
    public TextView textView;
  }

  public void clear(){
    mDataList.clear();
    notifyDataSetChanged();
  }

  public void addData(int index, SampleMaster sampleMaster){
    mDataList.add(index, sampleMaster);
    notifyDataSetChanged();
  }
  public void addData(SampleMaster sampleMaster){
    mDataList.add(sampleMaster);
    notifyDataSetChanged();
  }
  public void moveToTop(SampleMaster sampleMaster){
    int index = mDataList.indexOf(sampleMaster);
    mDataList.remove(index);
    mDataList.add(0, sampleMaster);
    notifyDataSetChanged();
  }


}
