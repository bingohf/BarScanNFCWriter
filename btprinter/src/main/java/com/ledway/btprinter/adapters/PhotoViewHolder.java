package com.ledway.btprinter.adapters;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;

import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.views.MImageView;
import java.io.File;

/**
 * Created by togb on 2016/6/4.
 */
public class PhotoViewHolder extends BaseViewHolder{
  private MImageView imageView;
  public PhotoViewHolder(View itemView) {
    super(itemView);
    imageView = (MImageView) itemView.findViewById(R.id.image_photo);
  }

  @Override public void changeData(BaseData baseData) {
    PhotoData photoData = (PhotoData) baseData;
    if (photoData.getBitmap() == null) {
      File file = new File(photoData.getBitmapPath());
      if (file.exists() && file.length() > 0) {
        int targetW = MApp.getApplication().point.x - 200;
        int targetH = Math.max(imageView.getHeight(),MApp.getApplication().point.y/3);
        Bitmap bitmap = photoData.getFileBitmap(targetW, targetH);
        if (bitmap != null) {
          imageView.setImageBitmap(bitmap);
          imageView.setImagePath(photoData.getBitmapPath());
          ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
          layoutParams.width = bitmap.getWidth();
          layoutParams.height = bitmap.getHeight();
        }else{
          imageView.setImageBitmap(null);
        }

      } else {
        imageView.setImageBitmap(null);
      }
    }else{
      imageView.setImageBitmap(photoData.getBitmap());
      imageView.setImagePath(null);
    }
  }

}
