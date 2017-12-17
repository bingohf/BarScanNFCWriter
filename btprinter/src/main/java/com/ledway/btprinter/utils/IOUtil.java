package com.ledway.btprinter.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import com.example.android.common.logger.Log;
import java.io.*;

public class IOUtil {

    public static byte[] readFile(String file) throws IOException {
        return readFile(new File(file));
    }

    public static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength) {
                throw new IOException("File size >= 2 GB");
            }
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

    public static Bitmap loadImage(String path, int width ,int height){
        int targetW = width;
        int targetH = height;


        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        return  BitmapFactory.decodeFile(path, bmOptions);
}

   public static void cropImage(File sourceImage){
        final int size = 512;
       try {

           BitmapFactory.Options tmpOptions = new BitmapFactory.Options();
           tmpOptions.inJustDecodeBounds = true;
           BitmapFactory.decodeStream( new FileInputStream(sourceImage), null, tmpOptions);
           int width = tmpOptions.outWidth;
           int height = tmpOptions.outHeight;

           int resizeWidth = Math.min(size, width);
           int resizeHeight = Math.min(size, height);
           float scaleWidth = ((float) resizeWidth) / width;
           float scaleHeight = ((float) resizeHeight) / height;
           float scale = Math.max(scaleWidth, scaleHeight);
           Bitmap b= BitmapFactory.decodeFile(sourceImage.getAbsolutePath());



           Bitmap out = Bitmap.createScaledBitmap(b,(int) (scale * width), (int)(scale *height), false);
           int x = (out.getWidth() - resizeWidth) /2;
           int y = (out.getHeight() - resizeHeight) /2;
           Bitmap dstBmp = Bitmap.createBitmap(out, x, y, resizeWidth, resizeHeight);
           dstBmp.compress(Bitmap.CompressFormat.JPEG, 70, new FileOutputStream(sourceImage));
           dstBmp.recycle();
           b.recycle();
           out.recycle();



/*           BitmapRegionDecoder bitmapRegionDecoder = BitmapRegionDecoder.newInstance( new FileInputStream(sourceImage),false);
           BitmapFactory.Options options1 = new BitmapFactory.Options();
           options1.inPreferredConfig = Bitmap.Config.RGB_565;

           Rect rect = new Rect(x, y, x + resizeWidth, y + resizeHeight);
           Bitmap bitmap = bitmapRegionDecoder.decodeRegion(rect, options1);*/

       } catch (IOException e) {
           e.printStackTrace();
           Log.e("cropImage", e.getMessage(), e);
       }
   }

}
