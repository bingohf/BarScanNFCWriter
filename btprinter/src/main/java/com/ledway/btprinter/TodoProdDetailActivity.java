package com.ledway.btprinter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.ledway.btprinter.models.TodoProd;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/7/3.
 */
public class TodoProdDetailActivity extends AppCompatActivity {
  private static final int REQUEST_TAKE_IMAGE = 1;

  private ImageView mImageView;
  private TextView mTxtHint;
  private TodoProd mTodoProd ;

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.todo_prod_menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.action_re_take_photo:{
        startTakePhoto();
        break;
      }
    }
    return true;
  }

  private void startTakePhoto(){
    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    startActivityForResult(takePicture, REQUEST_TAKE_IMAGE);
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_todo_prod_detail);
    mImageView = (ImageView) findViewById(R.id.image);
    mTxtHint = (TextView) findViewById(R.id.txt_hint);
    mTodoProd = (TodoProd) MApp.getApplication().getSession().getValue("current_todo_prod");

    if (mTodoProd.image1.length < 1){
      mTxtHint.setVisibility(View.VISIBLE);
      mImageView.setVisibility(View.GONE);
    }else{
      mImageView.setVisibility(View.VISIBLE);
      mTxtHint.setVisibility(View.GONE);

      Bitmap bitmap =  BitmapFactory.decodeByteArray(mTodoProd.image1 , 0, mTodoProd.image1 .length);
      mImageView.setImageBitmap(bitmap);
    }
    mTxtHint.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startTakePhoto();
      }
    });


/*    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    startActivityForResult(takePicture, REQUEST_TAKE_IMAGE);//zero can be replaced with any action code*/
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode){
      case REQUEST_TAKE_IMAGE:{
        if (RESULT_OK ==  resultCode){
          Bitmap photo = (Bitmap) data.getExtras().get("data");
          mImageView.setImageBitmap(photo);
          mImageView.setVisibility(View.VISIBLE);
          mTxtHint.setVisibility(View.GONE);

          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          photo.compress(Bitmap.CompressFormat.PNG, 100, stream);
          byte[] byteArray = stream.toByteArray();
          mTodoProd.image1 = byteArray;


          Bitmap resized = Bitmap.createScaledBitmap(photo, 110, 110, true);
          ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
          resized.compress(Bitmap.CompressFormat.PNG, 100, stream2);
          mTodoProd.image2 = stream2.toByteArray();
          mTodoProd.save();

          final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.upload), getString(R.string.wait_a_moment), true);
          mTodoProd.remoteSave().subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new Subscriber<ArrayList<Object>>() {
                @Override public void onCompleted() {
                  progressDialog.dismiss();
                }

                @Override public void onError(Throwable e) {
                  progressDialog.dismiss();
                  Toast.makeText(TodoProdDetailActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                  e.printStackTrace();
                }

                @Override public void onNext(ArrayList<Object> objects) {
                  int returnCode = (Integer) objects.get(0);
                  String returnMessage = (String) objects.get(1);
                  if (!TextUtils.isEmpty(returnMessage)){
                    Toast.makeText(TodoProdDetailActivity.this, returnMessage, Toast.LENGTH_LONG).show();
                  }else{
                    mTodoProd.uploaded_time = new Date();
                    mTodoProd.save();
                  }

                }
              });
        }
        break;
      }
    }
  }
}
