package com.ledway.btprinter.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;

/**
 * Created by togb on 2016/6/4.
 */
public class TextViewHolder extends BaseViewHolder {
  private TextView mTxtLabel;
  private TextView mTxtValue;
  public TextViewHolder(View itemView) {
    super(itemView);
    mTxtLabel = (TextView) itemView.findViewById(R.id.txt_label);
    mTxtValue = (TextView) itemView.findViewById(R.id.txt_value);
  }

  @Override public void changeData(BaseData textData) {
    String label ="";
    switch (textData.getType()){
      case DataAdapter.DATA_TYPE_BARCODE:{
        //label = MApp.getApplication().getString(R.string.prod);
        break;
      }
      case DataAdapter.DATA_TYPE_MEMO:{
        label = MApp.getApplication().getString(R.string.qr_card);
        break;
      }

    }
    mTxtLabel.setText(label);
    mTxtValue.setText(((TextData)textData).getText());
  }
}
