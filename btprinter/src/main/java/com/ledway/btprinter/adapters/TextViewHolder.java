package com.ledway.btprinter.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by togb on 2016/6/4.
 */
public class TextViewHolder extends BaseViewHolder {
  private TextView mTextView;
  public TextViewHolder(View itemView) {
    super(itemView);
    mTextView = (TextView) itemView;
  }

  @Override public void changeData(BaseData textData) {
    mTextView.setText(((TextData)textData).getText());
  }
}
