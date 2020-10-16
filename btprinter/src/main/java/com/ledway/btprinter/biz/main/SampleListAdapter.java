package com.ledway.btprinter.biz.main;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
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
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SampleListAdapter  extends RecyclerView.Adapter<SampleListAdapter.SampleViewHolder>
    implements View.OnClickListener {
  private SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault());
  private List<SampleMaster> mData = new ArrayList<>();
  private final LayoutInflater mLayoutInflater;
  private PublishSubject<SampleMaster> mClickSubject =  PublishSubject.create();

  public SampleListAdapter(Context context) {
    mLayoutInflater = LayoutInflater.from(context);
  }


  @Override public SampleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = mLayoutInflater.inflate(R.layout.list_item_sample, parent, false);
    view.setOnClickListener(this);
    return new SampleViewHolder(view);
  }

  @Override public void onBindViewHolder(SampleViewHolder holder, int position) {
    SampleMaster dataItem = mData.get(position);
    if(!TextUtils.isEmpty(dataItem.getImage1())) {
      File file = new File(dataItem.getImage1());
      if (file.exists()) {
        Picasso.with(holder.itemView.getContext()).load(file).fit().into(holder.imgIcon);
      }
    }
    holder.txtTitle.setText(safeText(dataItem.getDesc()));
    holder.txtSubTitle.setText("");
    holder.txtTimestamp.setText(formatDate(dataItem.update_date));
    holder.imgSynced.setVisibility(dataItem.isDirty ? View.VISIBLE:View.GONE);
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
  public void setData(List<SampleMaster> data){
    mData = data;
  }

  public PublishSubject<SampleMaster> getClickObservable(){
    return mClickSubject;
  }

  @Override public int getItemCount() {
    return mData.size();
  }

  @Override public void onClick(View view) {
    SampleMaster item = mData.get((int)view.getTag());
    mClickSubject.onNext(item);
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


}
