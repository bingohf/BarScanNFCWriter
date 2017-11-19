package com.ledway.btprinter.biz.main;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.ledway.btprinter.R;
import com.ledway.btprinter.models.SampleMaster;
import com.squareup.picasso.Picasso;
import io.reactivex.subjects.PublishSubject;
import java.io.DataInput;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SampleListAdapter2 extends RecyclerView.Adapter<SampleListAdapter2.SampleViewHolder>
    implements View.OnClickListener {
  private SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault());
  private List<ItemData> mData = new ArrayList<>();
  private final LayoutInflater mLayoutInflater;
  private PublishSubject<Object> mClickSubject =  PublishSubject.create();

  public SampleListAdapter2(Context context) {
    mLayoutInflater = LayoutInflater.from(context);
  }


  @Override public SampleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = mLayoutInflater.inflate(R.layout.list_item_sample, parent, false);
    view.setOnClickListener(this);
    return new SampleViewHolder(view);
  }

  @Override public void onBindViewHolder(SampleViewHolder holder, int position) {
    ItemData dataItem = mData.get(position);
    if(!TextUtils.isEmpty(dataItem.iconPath)) {
      File file = new File(dataItem.iconPath);
      if (file.exists()) {
        Picasso.with(holder.itemView.getContext()).load(file).fit().into(holder.imgIcon);
      }
    }
    holder.txtTitle.setText(safeText(dataItem.title));
    holder.txtSubTitle.setText("");
    holder.txtTimestamp.setText(formatDate(dataItem.timestamp));
    holder.imgSynced.setVisibility(dataItem.redFlag ? View.VISIBLE:View.GONE);
    holder.itemView.setTag(position);
  }

  private String formatDate(Date date){
    return mDateFormatter.format(date);
  }


  private String safeText(String text){
    if(TextUtils.isEmpty(text)){
      return "NA";
    }
    return text;
  }
  public void setData(List<ItemData> data){
    mData = data;
  }

  public PublishSubject<Object> getClickObservable(){
    return mClickSubject;
  }

  @Override public int getItemCount() {
    return mData.size();
  }

  @Override public void onClick(View view) {
    ItemData item = mData.get((int) view.getTag());
    mClickSubject.onNext(item.hold);
  }

  public static class SampleViewHolder extends RecyclerView.ViewHolder{
    @BindView(R.id.icon) ImageView imgIcon;
    @BindView(R.id.txt_title) TextView txtTitle;
    @BindView(R.id.txt_sub_title) TextView txtSubTitle;
    @BindView(R.id.txt_timestamp) TextView txtTimestamp;
    @BindView(R.id.img_synced) View imgSynced;
    public SampleViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  public static class ItemData<T>{
    public String iconPath;
    public Date timestamp;
    public String title;
    public String subTitle;
    public boolean redFlag;
    public T hold;
  }



}
