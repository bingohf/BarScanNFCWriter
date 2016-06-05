package com.ledway.btprinter.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import com.ledway.btprinter.R;

/**
 * Created by togb on 2016/6/4.
 */
public class PhotoViewHolder extends BaseViewHolder{
  private ImageView imageView;
  public PhotoViewHolder(View itemView) {
    super(itemView);
    imageView = (ImageView) itemView.findViewById(R.id.image_photo);
  }

  @Override public void changeData(BaseData baseData) {
    PhotoData photoData = (PhotoData) baseData;
    imageView.setImageBitmap(photoData.getBitmap());
  }
}
