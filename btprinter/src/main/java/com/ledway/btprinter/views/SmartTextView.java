package com.ledway.btprinter.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by togb on 2016/9/25.
 */

public class SmartTextView extends TextView {
  public SmartTextView(Context context) {
    super(context);
  }

  public SmartTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SmartTextView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override public void setText(CharSequence text, BufferType type) {
    super.setText(text, type);
    if(TextUtils.isEmpty(text)){
      setVisibility(GONE);
    }else{
      setVisibility(VISIBLE);
    }
  }
}
