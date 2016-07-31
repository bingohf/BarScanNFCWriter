package com.ledway.btprinter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.btprinter.utils.IOUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
  private EditText mEdtSpec;
  private String mCurrentPhotoPath;

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.todo_prod_menu, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.action_re_take_photo:{
        startTakePhoto(1);
        break;
      }
      case R.id.action_re_upload:{
        upload();
        break;
      }
    }
    return true;
  }


  @Override public void onBackPressed() {
    if (!mEdtSpec.getText().toString().equals(mTodoProd.spec_desc)){
      mTodoProd.uploaded_time = null;
    }
    mTodoProd.spec_desc = mEdtSpec.getText().toString();
    mTodoProd.save();
    super.onBackPressed();
  }

  @Override protected void onDestroy() {
    super.onDestroy();

    mTodoProd.image1 = null;
    mTodoProd.image2 = null;
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_todo_prod_detail);
    mImageView = (ImageView) findViewById(R.id.image);
    mTxtHint = (TextView) findViewById(R.id.txt_hint);
    mEdtSpec = (EditText) findViewById(R.id.txt_spec);
    mTodoProd = (TodoProd) MApp.getApplication().getSession().getValue("current_todo_prod");
    mTodoProd.queryAllField();
    getSupportActionBar().setTitle(mTodoProd.prodNo);
    mEdtSpec.setText(mTodoProd.spec_desc);
    if (TextUtils.isEmpty(mTodoProd.image1)){
      mTxtHint.setVisibility(View.VISIBLE);
      mImageView.setVisibility(View.GONE);
    }else{
      mImageView.setVisibility(View.VISIBLE);
      mTxtHint.setVisibility(View.GONE);

      Bitmap bitmap =  IOUtil.loadImage(mTodoProd.image1, 800,800);
      mImageView.setImageBitmap(bitmap);
    }
    mTxtHint.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        startTakePhoto(1);
      }
    });
    mEdtSpec.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus){
          mTodoProd.spec_desc = mEdtSpec.getText().toString();
          mTodoProd.uploaded_time = null;
          mTodoProd.save();
        }
      }
    });

/*    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    startActivityForResult(takePicture, REQUEST_TAKE_IMAGE);//zero can be replaced with any action code*/
  }

  @Override protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
    switch (requestCode){
      case REQUEST_TAKE_IMAGE:{
        if (RESULT_OK ==  resultCode){
          File f = new File(mCurrentPhotoPath);
          if (f.exists()){
            if (f.length() < 1){
              f.delete();
            }
          }
          if (f.exists()){
            mTodoProd.image1 = mCurrentPhotoPath;
            Bitmap bitmap = IOUtil.loadImage(mCurrentPhotoPath, 800, 800);
            File file2 = new File(MApp.getApplication().getPicPath() + "/" + getProdNoFileName() + "_type_" + 2 +".jpeg");
            if (!file2.exists()){
              try {
                file2.createNewFile();
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
            try {
              OutputStream outputStream = new FileOutputStream(file2);
              Bitmap resized = Bitmap.createScaledBitmap(bitmap, 110, 110, true);
              resized.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
              mTodoProd.image2 = file2.getAbsolutePath();
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            }
            mImageView.setVisibility(View.VISIBLE);
            mTxtHint.setVisibility(View.GONE);
            mImageView.setImageBitmap(bitmap);
            mTodoProd.uploaded_time = null;
            mTodoProd.save();
          }
       //   upload();

        }
        break;
      }
    }
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
    //menu.findItem(R.id.action_re_upload).setVisible(mTodoProd.image1 != null && mTodoProd.image1.length > 0);
    return true;
  }

  private void upload(){
    mTodoProd.spec_desc = mEdtSpec.getText().toString();
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
            }
            if (returnCode == 1){
              mTodoProd.uploaded_time = new Date();
              mTodoProd.save();
            }
            invalidateOptionsMenu();

          }
        });
  }


  private void startTakePhoto(int type){
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      File photoFile = null;
      try {
        photoFile =
            new File(MApp.getApplication().getPicPath() + "/" + getProdNoFileName() + "_type_" + type +".jpeg");
        mCurrentPhotoPath =  photoFile.getAbsolutePath();
        photoFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (photoFile != null) {
        Uri photoURI =
            FileProvider.getUriForFile(TodoProdDetailActivity.this, "com.ledway.btprinter.fileprovider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, type);
      }
    }
  }

  private String getProdNoFileName(){
    return mTodoProd.prodNo.replace("[\\*\\/\\\\\\?]","_");
  }

}
