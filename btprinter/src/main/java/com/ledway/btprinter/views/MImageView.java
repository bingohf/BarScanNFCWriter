package com.ledway.btprinter.views;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import java.io.File;

/**
 * Created by togb on 2016/8/6.
 */
public class MImageView extends ImageView {
  private String imagePath;

  public MImageView(Context context) {
    this(context, null);
  }

  public MImageView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public MImageView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        if(TextUtils.isEmpty(imagePath)){
          return;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(FileProvider.getUriForFile(getContext(),getContext().getPackageName()+".provider",new File(imagePath)), "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getContext().startActivity(intent);
      }
    });
  }

  public void setImagePath(String path){
    this.imagePath =path;
  }
}
