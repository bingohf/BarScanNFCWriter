package com.ledway.btprinter.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import java.io.File;

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
    if (photoData.getBitmap() == null) {
      File file = new File(photoData.getBitmapPath());
      if (file.exists() && file.length() > 0) {
        int targetW = Math.max(imageView.getWidth(), 800);
        int targetH = Math.max(imageView.getHeight(),800);

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoData.getBitmapPath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(photoData.getBitmapPath(), bmOptions);
        imageView.setImageBitmap(bitmap);
      } else {
        imageView.setImageBitmap(null);
      }
    }else{
      imageView.setImageBitmap(photoData.getBitmap());
    }
  }

}
