package com.ledway.btprinter.biz.main;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.ledway.btprinter.R;
import com.squareup.picasso.Picasso;
import io.reactivex.subjects.PublishSubject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SampleListAdapter2 extends RecyclerView.Adapter<SampleListAdapter2.SampleViewHolder>
    implements View.OnClickListener, View.OnLongClickListener {
  private final LayoutInflater mLayoutInflater;
  private SimpleDateFormat mDateFormatter =
      new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
  private List<ItemData> mData;
  private PublishSubject<Object> mClickSubject = PublishSubject.create();
  private PublishSubject<Object> mLongClickSubject = PublishSubject.create();
  private PublishSubject<Pair<Integer,Boolean>> mCheckSubject = PublishSubject.create();
  private boolean selectMode = false;

  public SampleListAdapter2(Context context) {
    this(context, new ArrayList<>());
  }

  public SampleListAdapter2(Context context, List<ItemData> data) {
    mLayoutInflater = LayoutInflater.from(context);
    mData = data;
  }

  @Override public SampleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = mLayoutInflater.inflate(R.layout.list_item_sample, parent, false);
    view.setOnClickListener(this);
    view.setOnLongClickListener(this);
    SampleViewHolder hold = new SampleViewHolder(view);
    hold.checkBox.setOnCheckedChangeListener(
        (compoundButton, b) -> {
          if(hold.position >-1) {
            mData.get(hold.position).isChecked = b;
            mCheckSubject.onNext(new Pair<>(hold.position, b));
          }


        });
    return hold;
  }

  @Override public void onBindViewHolder(SampleViewHolder holder, int position) {
    ItemData dataItem = mData.get(position);
    holder.imgIcon.setImageDrawable(
        ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_file_image_black_36dp));
    if (!TextUtils.isEmpty(dataItem.iconPath)) {
      File file = new File(dataItem.iconPath);
      if (file.exists()) {
        Picasso.with(holder.itemView.getContext()).load(file).fit().into(holder.imgIcon);
      }
    }
    holder.txtTitle.setText(safeText(dataItem.title));
    holder.txtSubTitle.setVisibility(TextUtils.isEmpty(dataItem.subTitle) ? View.GONE:View.VISIBLE);
    holder.txtSubTitle.setText(safeText(dataItem.subTitle));
    holder.txtTimestamp.setText(formatDate(dataItem.timestamp));
    holder.txtTimestamp.setVisibility(dataItem.timestamp== null ?View.GONE:View.VISIBLE);
    holder.imgSynced.setVisibility(dataItem.redFlag ? View.VISIBLE : View.GONE);
    holder.checkBox.setVisibility(selectMode ? View.VISIBLE : View.GONE);

    holder.position = position;
    holder.itemView.setTag(position);
    holder.checkBox.setChecked(dataItem.isChecked);
  }

  private String formatDate(Date date) {
    if (date == null) return "";
    return mDateFormatter.format(date);
  }

  private String safeText(String text) {
    if (TextUtils.isEmpty(text)) {
      return "NA";
    }
    return text;
  }

  @Override public int getItemCount() {
    return mData.size();
  }

  public void setSelectMode(boolean enable) {
    selectMode = enable;
  }

  public void setData(List<ItemData> data) {
    mData = data;
  }

  public PublishSubject<Object> getClickObservable() {
    return mClickSubject;
  }
  public PublishSubject<Object> getmLongClickSubject() {
    return mLongClickSubject;
  }

  public PublishSubject<Pair<Integer, Boolean>> getCheckObservable() {
    return mCheckSubject;
  }

  @Override public void onClick(View view) {
    ItemData item = mData.get((int) view.getTag());
    if (item.hold != null) {
      mClickSubject.onNext(item.hold);
    }
  }

  public ItemData get(int position){
    return mData.get(position);
  }


  public ItemData[] getSelection() {
    ArrayList<ItemData> temp = new ArrayList<>();
    for (ItemData itemData : mData) {
      if (itemData.isChecked) {
        temp.add(itemData);
      }
    }
    ItemData[] ret = new ItemData[temp.size()];
    ret = temp.toArray(ret);
    return ret;
  }

  @Override public boolean onLongClick(View view) {
  //  ItemData item = mData.get((int) view.getTag());
    mLongClickSubject.onNext(view);
    return true;
  }

  public static class SampleViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.icon) ImageView imgIcon;
    @BindView(R.id.txt_title) TextView txtTitle;
    @BindView(R.id.txt_sub_title) TextView txtSubTitle;
    @BindView(R.id.txt_timestamp) TextView txtTimestamp;
    @BindView(R.id.img_synced) View imgSynced;
    @BindView(R.id.checkbox) CheckBox checkBox;
    public int position = -1;
    public SampleViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  public static class ItemData<T> {
    public String iconPath;
    public Date timestamp;
    public String title;
    public String subTitle;
    public boolean redFlag;
    public boolean isChecked;
    public T hold;
  }
}
