package com.ledway.btprinter.adapters;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import com.ledway.btprinter.R;

/**
 * Created by togb on 2016/7/3.
 */

public class ToProdViewHolder  extends RecyclerView.ViewHolder{
  public TextView textView;
  public ToProdViewHolder(View itemView) {
    super(itemView);
    textView = (TextView) itemView.findViewById(R.id.txtView);
  }
}
