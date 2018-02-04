package com.ledway.btprinter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.common.logger.Log;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.btprinter.network.model.ProductReturn;
import com.ledway.btprinter.network.model.RestDataSetResponse;
import com.ledway.scanmaster.utils.IOUtil;
import com.ledway.btprinter.views.MImageView;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by togb on 2016/9/25.
 */

public class RemoteProdActivity extends AppCompatActivity {
  private TextView mTxtSpec;
  private MImageView mImageView;
  private TextView mImageHintView;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_remote_product);
    String remoteDeviceId = getIntent().getStringExtra("deviceId");
    String prodno = getIntent().getStringExtra("prodno");
    mTxtSpec = (TextView) findViewById(R.id.txt_spec);
    mImageView = (MImageView) findViewById(R.id.image);
    mImageHintView = (TextView) findViewById(R.id.txt_hint);

    getSupportActionBar().setTitle(prodno);
    loadRemoteData(remoteDeviceId, prodno);
  }

  private void loadRemoteData(final String remoteDeviceId, final String prodno) {
    final ProgressDialog progressDialog =
        ProgressDialog.show(this, getString(R.string.loading), getString(R.string.wait_a_moment),
            true, true);

    String query = String.format("empno ='%s' and prodno ='%s'", remoteDeviceId, prodno);
    final Subscription subscription = MyProjectApi.getInstance()
        .getDbService()
        .getProduct(query, "")
        .map(new Func1<RestDataSetResponse<ProductReturn>, TodoProd>() {
          @Override public TodoProd call(RestDataSetResponse<ProductReturn> response) {
            TodoProd todoProd = new TodoProd();
            try {

              for (ProductReturn item : response.result.get(0)) {
                todoProd.prodNo = prodno;
                todoProd.spec_desc = item.specdesc;
                File imageFile = new File(MApp.getApplication().getPicPath()
                    + "/"
                    + remoteDeviceId
                    + "_"
                    + prodno.replaceAll("[\\*\\/\\\\\\?]", "_")
                    + "_"
                    + ".jpeg");
                if (imageFile.exists()) {
                  imageFile.delete();
                }
                InputStream inputStream =
                    new ByteArrayInputStream(Base64.decode(item.graphic, Base64.DEFAULT));
                if (inputStream != null && inputStream.available() > 0) {
                  FileOutputStream outputStream = new FileOutputStream(imageFile);
                  byte[] buffer = new byte[1024];
                  int read;
                  while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                  }
                  outputStream.flush();
                  outputStream.close();
                  todoProd.image1 = imageFile.getAbsolutePath();
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
            return todoProd;
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<TodoProd>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
          }

          @Override public void onError(Throwable e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("error", e.getMessage(), e);
            progressDialog.dismiss();
          }

          @Override public void onNext(TodoProd todoProd) {
            mTxtSpec.setText(todoProd.spec_desc);
            if (!TextUtils.isEmpty(todoProd.image1)) {
              Bitmap bitmap = IOUtil.loadImage(todoProd.image1, 800, 800);
              mImageView.setImageBitmap(bitmap);
              mImageView.setImagePath(todoProd.image1);
              mImageHintView.setVisibility(View.GONE);
            }
          }
        });

    progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override public void onCancel(DialogInterface dialog) {
        subscription.unsubscribe();
        finish();
      }
    });
  }
}
