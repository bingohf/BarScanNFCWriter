package com.ledway.btprinter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.Image;
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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.activeandroid.query.Select;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.btprinter.network.model.RestSpResponse;
import com.ledway.btprinter.network.model.SpReturn;
import com.ledway.btprinter.utils.IOUtil;
import com.ledway.btprinter.views.MImageView;
import com.ledway.framework.FullScannerActivity;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/7/3.
 */
public class TodoProdDetailActivity extends AppCompatActivity {
  private static final int REQUEST_TAKE_IMAGE = 1;
  private static final int RESULT_CAMERA_QR_CODE = 2;

  private TodoProd mTodoProd ;
  private String mCurrentPhotoPath;
  @BindView(R.id.image) MImageView mImageView;
  @BindView(R.id.txt_hint) TextView mTxtHint;
  @BindView(R.id.txt_spec) EditText mEdtSpec;
  @BindView(R.id.img_qrcode) ImageView mImgQrCode;
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
    if(mTodoProd.update_time.getTime() != mTodoProd.create_time.getTime()) {
      mTodoProd.save();
    }
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
    ButterKnife.bind(this);

    loadTodoProd();
    //mTodoProd.queryAllField();
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
      mImageView.setImagePath(mTodoProd.image1);
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
        }
      }
    });

/*    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    startActivityForResult(takePicture, REQUEST_TAKE_IMAGE);//zero can be replaced with any action code*/
  }

  @OnTextChanged(R.id.txt_spec) void onTextSpecChange(){
    if(!mEdtSpec.getText().toString().equals(mTodoProd.spec_desc)){
      mTodoProd.update_time = new Date();
    }
    mTodoProd.spec_desc = mEdtSpec.getText().toString();
  }

  @OnClick(R.id.img_qrcode) void onImgQrCodeClick(){
    startActivityForResult(new Intent(this, FullScannerActivity.class),
        RESULT_CAMERA_QR_CODE);
  }

  private void loadTodoProd() {
    String prodno = getIntent().getStringExtra("prod_no");
    if(TextUtils.isEmpty(prodno)) {
      mTodoProd = (TodoProd) MApp.getApplication().getSession().getValue("current_todo_prod");
    }else {
      mTodoProd = loadTodoProd(prodno);
    }
  }

  private TodoProd loadTodoProd(String prodno) {
    List<TodoProd> todoProds = new Select().from(TodoProd.class).where("prodno =?", prodno).execute();
    if (todoProds.size() > 0){
      return todoProds.get(0);
    }
    TodoProd todoProd = new TodoProd();
    todoProd.create_time = new Date();
    todoProd.update_time = todoProd.create_time;
    todoProd.prodNo = prodno;
    return todoProd;
  }

  @Override protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
    switch (requestCode){
      case RESULT_CAMERA_QR_CODE:{
        if(resultCode == Activity.RESULT_OK) {
          String qrcode = data.getStringExtra("barcode");
          mEdtSpec.setText(qrcode);
        }
        break;
      }
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
              float rate = 110f / bitmap.getWidth();
              Bitmap resized = Bitmap.createScaledBitmap(bitmap, 110,
                  (int) (rate * bitmap.getHeight()), true);
              resized.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
              mTodoProd.image2 = file2.getAbsolutePath();
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            }
            mImageView.setVisibility(View.VISIBLE);
            mTxtHint.setVisibility(View.GONE);
            mImageView.setImageBitmap(bitmap);
            mImageView.setImagePath(f.getAbsolutePath());
            mTodoProd.update_time = new Date();
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
    mTodoProd.remoteSave2().subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<RestSpResponse<SpReturn>>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
          }

          @Override public void onError(Throwable e) {
            progressDialog.dismiss();
            Toast.makeText(TodoProdDetailActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
          }

          @Override public void onNext(RestSpResponse<SpReturn> spReturnRestSpResponse) {
            int returnCode = spReturnRestSpResponse.result.get(0).errCode;
            String returnMessage =  spReturnRestSpResponse.result.get(0).errData;
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
            FileProvider.getUriForFile(TodoProdDetailActivity.this, BuildConfig.APPLICATION_ID+".fileprovider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        List<ResolveInfo> resolvedIntentActivities = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
          String packageName = resolvedIntentInfo.activityInfo.packageName;

          grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivityForResult(takePictureIntent, type);
      }
    }
  }

  private String getProdNoFileName(){
    return mTodoProd.prodNo.replaceAll("[\\*\\/\\\\\\?]","_");
  }

}
