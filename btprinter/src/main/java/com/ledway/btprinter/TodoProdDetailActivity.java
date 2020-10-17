package com.ledway.btprinter;

import android.app.Activity;
import android.app.ProgressDialog;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.activeandroid.query.Select;
import com.example.android.common.logger.Log;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxItemDecoration;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.ledway.btprinter.event.ProdSaveEvent;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.btprinter.network.model.RestSpResponse;
import com.ledway.btprinter.network.model.SpReturn;
import com.ledway.btprinter.views.MImageView;
import com.ledway.framework.FullScannerActivity;
import com.ledway.rxbus.RxBus;
import com.ledway.scanmaster.ApplicationContext;
import com.ledway.scanmaster.model.OCRData;
import com.ledway.scanmaster.model.Resource;
import com.ledway.scanmaster.utils.BizUtils;
import com.ledway.scanmaster.utils.ContextUtils;
import com.ledway.scanmaster.utils.IOUtil;
import com.ledway.scanmaster.utils.JsonUtils;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by togb on 2016/7/3.
 */
public class TodoProdDetailActivity extends AppCompatActivity {
  private static final int REQUEST_TAKE_IMAGE = 1;
  private static final int RESULT_CAMERA_QR_CODE = 2;
  private static final int REQUEST_TAKE_FOR =3000;
  @BindView(R.id.txt_spec) EditText mEdtSpec;
  @BindView(R.id.img_qrcode) ImageView mImgQrCode;
  @BindView(R.id.list_view)  RecyclerView mListView;
  private TodoProd mTodoProd;
  private String mCurrentPhotoPath;
  private String mMyTaxNo;
  private MutableLiveData<Resource<OCRData>> orc = new MutableLiveData<>();
  private ProgressDialog mProgressDialog;
  public static String[]  pictureTypes = new String[]{"Main" , "Left" ,"Flat", "Down", "Front", "Bent", "Right"};
  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.todo_prod_menu, menu);
    return true;
  }

  @Override public boolean onPrepareOptionsMenu(Menu menu) {
    //menu.findItem(R.id.action_re_upload).setVisible(mTodoProd.image1 != null && mTodoProd.image1.length > 0);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_re_take_photo: {
        takePictureFor(0);
        break;
      }
      case R.id.action_re_upload: {
        upload();
        break;
      }
    }
    return true;
  }

  private void upload() {
    mTodoProd.spec_desc = mEdtSpec.getText().toString();
    mTodoProd.save();
    final ProgressDialog progressDialog =
        ProgressDialog.show(this, getString(R.string.upload), getString(R.string.wait_a_moment),
            true);
    mTodoProd.remoteSave3()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<String>() {
          @Override public void onCompleted() {
            progressDialog.dismiss();
          }

          @Override public void onError(Throwable e) {
            progressDialog.dismiss();
            Toast.makeText(TodoProdDetailActivity.this, ContextUtils.getMessage(e),
                Toast.LENGTH_LONG).show();
            e.printStackTrace();
          }

          @Override public void onNext(String returnMessage) {
            Toast.makeText(TodoProdDetailActivity.this, returnMessage, Toast.LENGTH_LONG).show();
            mTodoProd.uploaded_time = new Date();
            mTodoProd.save();
            invalidateOptionsMenu();
          }
        });
  }

  private void startTakePhoto(int type) {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      File photoFile = null;
      try {
        photoFile = new File(MApp.getApplication().getPicPath()
            + "/"
            + getProdNoFileName()
            + "_type_"
            + type
            + ".jpeg");
        mCurrentPhotoPath = photoFile.getAbsolutePath();
        photoFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (photoFile != null) {
        Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
            BuildConfig.APPLICATION_ID + ".provider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        List<ResolveInfo> resolvedIntentActivities =
            getPackageManager().queryIntentActivities(takePictureIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
          String packageName = resolvedIntentInfo.activityInfo.packageName;

/*          grantUriPermission(packageName, photoURI,
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);*/
        }
        startActivityForResult(takePictureIntent, type);
      }
    }
  }

  public static String getImagePath(String prodno ,String pictureType, int type){
    if ("Main".equals(pictureType)) {
      return MApp.getApplication().getPicPath()
              + "/"
              + prodno.replaceAll("[\\*\\/\\\\\\?]", "_")
              + "_type_"
              + type
              + ".jpeg";
    }
    return MApp.getApplication().getPicPath()
            + "/"
            + prodno.replaceAll("[\\*\\/\\\\\\?]", "_")
            + "_" + pictureType + "_"
            + type
            + ".jpeg";
  }

  private String getProdNoFileName() {
    return mTodoProd.prodNo.replaceAll("[\\*\\/\\\\\\?]", "_");
  }

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_todo_prod_detail);
    ButterKnife.bind(this);

    bindListView();

    loadTodoProd();
    //mTodoProd.queryAllField();
    getSupportActionBar().setTitle(mTodoProd.prodNo);
    mEdtSpec.setText(mTodoProd.spec_desc);
    mEdtSpec.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          mTodoProd.spec_desc = mEdtSpec.getText().toString();
        }
      }
    });

    mEdtSpec.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override public void afterTextChanged(Editable editable) {
        if (!mEdtSpec.getText().toString().equals(mTodoProd.spec_desc)) {
          mTodoProd.update_time = new Date();
        }
        mTodoProd.spec_desc = mEdtSpec.getText().toString();
      }
    });

    mMyTaxNo = BizUtils.getMyTaxNo(this);

    orc.observe(this, ocrData -> {
      switch (ocrData.status) {
        case LOADING: {
          showLoading();
          break;
        }
        case ERROR: {
          stopLoading();
          Toast.makeText(this, ocrData.message, Toast.LENGTH_LONG).show();
          break;
        }
        case SUCCESS: {
          mEdtSpec.setText(ocrData.data.text);
          if (ocrData.data.limit - ocrData.data.count <= 100) {
            Toast.makeText(this,
                getString(R.string.ocr_count_limit, ocrData.data.count, ocrData.data.limit),
                Toast.LENGTH_LONG).show();
          }
          stopLoading();
          break;
        }
      }
    });

/*    Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    startActivityForResult(takePicture, REQUEST_TAKE_IMAGE);//zero can be replaced with any action code*/
  }

  private void bindListView() {
    FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);

    layoutManager.setFlexDirection(FlexDirection.ROW);
    layoutManager.setJustifyContent(JustifyContent.CENTER);


    FlexboxItemDecoration decor = new FlexboxItemDecoration(this);
    decor.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_flex));
    decor.setOrientation(FlexboxItemDecoration.BOTH);
    mListView.addItemDecoration(decor);

    mListView.setLayoutManager(layoutManager);
    mListView.setAdapter(new MyAdapter());

  }

  @Override protected void onDestroy() {
    super.onDestroy();

    mTodoProd.image1 = null;
    mTodoProd.image2 = null;
  }

  private void loadTodoProd() {

    String prodno = getIntent().getStringExtra("prod_no");
    String prodJson = getIntent().getStringExtra("prodJson");
    TodoProd defaultProd = null;
    if (!TextUtils.isEmpty(prodJson)){
      defaultProd = JsonUtils.Companion.fromJson(prodJson, TodoProd.class);
      prodno = defaultProd.prodNo;
    }
    if (TextUtils.isEmpty(prodno)) {
      mTodoProd = (TodoProd) MApp.getApplication().getSession().getValue("current_todo_prod");
    } else {
      mTodoProd = loadTodoProd(prodno,defaultProd);
    }
  }

  private TodoProd loadTodoProd(String prodno, TodoProd defaultProd) {
    List<TodoProd> todoProds =
        new Select().from(TodoProd.class).where("prodNo =?", prodno).execute();
    if (todoProds.size() > 0) {
      return todoProds.get(0);
    }
    if (defaultProd != null) return defaultProd;
    TodoProd todoProd = new TodoProd();
    todoProd.create_time = new Date();
    todoProd.update_time = todoProd.create_time;
    todoProd.prodNo = prodno;
    return todoProd;
  }

  private void showLoading() {
    mProgressDialog =
        ProgressDialog.show(this, getString(R.string.upload), getString(R.string.wait_a_moment),
            false);
    mProgressDialog.setOnDismissListener(dialogInterface -> mProgressDialog = null);
  }

  private void stopLoading() {
    if (mProgressDialog != null) {
      mProgressDialog.dismiss();
      mProgressDialog = null;
    }
  }



  @OnClick(R.id.img_qrcode) void onImgQrCodeClick() {
    startActivityForResult(new Intent(this, FullScannerActivity.class), RESULT_CAMERA_QR_CODE);
  }

  @Override protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case RESULT_CAMERA_QR_CODE: {
        if (resultCode == Activity.RESULT_OK) {
          String qrcode = data.getStringExtra("barcode");
          mEdtSpec.setText(qrcode);
        }
        break;
      }
      default:{
        if (requestCode >= REQUEST_TAKE_FOR){
          int typeIndex = requestCode - REQUEST_TAKE_FOR;
          if (RESULT_OK == resultCode) {
            File f = new File(mCurrentPhotoPath);
            TodoProd.setFileUpload(f, false);
            if (f.exists()) {
              if (f.length() < 1) {
                f.delete();
              }
            }
            if (f.exists()) {
              if(typeIndex ==0) {
                mTodoProd.image1 = mCurrentPhotoPath;
              }
              IOUtil.cropImage(new File(mCurrentPhotoPath));
              Picasso.with(this).invalidate(new File(mCurrentPhotoPath));
              Bitmap bitmap = IOUtil.loadImage(mCurrentPhotoPath, 800, 800);
              File file2 = new File(getImagePath(mTodoProd.prodNo,pictureTypes[typeIndex], 2));
              if (!file2.exists()) {
                try {
                  file2.createNewFile();
                } catch (IOException e) {
                  e.printStackTrace();
                }
              }
              try {
                OutputStream outputStream = new FileOutputStream(file2);
                float rate = 110f / bitmap.getWidth();
                Bitmap resized =
                        Bitmap.createScaledBitmap(bitmap, 110, (int) (rate * bitmap.getHeight()), true);
                resized.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                if(typeIndex ==0) {
                  mTodoProd.image2 = file2.getAbsolutePath();
                }
              } catch (FileNotFoundException e) {
                e.printStackTrace();
              }
              mTodoProd.update_time = new Date();
              mTodoProd.save();
              mListView.getAdapter().notifyItemChanged(typeIndex);
            }
            //   upload();

          }
        }
      }
    }
  }

  @Override public void onBackPressed() {
    if (mTodoProd.update_time != null
        && mTodoProd.create_time != null
        && mTodoProd.update_time.getTime() - mTodoProd.create_time.getTime() !=0) {
      mTodoProd.create_time = mTodoProd.update_time;
      mTodoProd.save();
      RxBus.getInstance().post(new ProdSaveEvent());
    }
    Intent data = new Intent();
    data.putExtra("prodJson", JsonUtils.Companion.toJson(mTodoProd));
    setResult(Activity.RESULT_OK,data);
    super.onBackPressed();
  }

  @OnClick(R.id.btn_ocr) void onBtnOCRClick() {
    if (!mEdtSpec.getText().toString().trim().isEmpty()) {
      Toast.makeText(this, R.string.hint_clear_description, Toast.LENGTH_SHORT).show();
      return;
    }
    if (mTodoProd.image1 == null) {
      Toast.makeText(this, R.string.no_image, Toast.LENGTH_SHORT).show();
      return;
    }
    ocrImage(mTodoProd.image1);
  }

  private void ocrImage(String fileName) {
    orc.setValue(Resource.loading(null));
    OkHttpClient client = new OkHttpClient.Builder()
        .addInterceptor(new HttpLoggingInterceptor(message ->{
          Log.d("OkHttp", message);
          Timber.tag("OkHttp").d(message);
        }).setLevel(HttpLoggingInterceptor.Level.BODY)).build();
    RequestBody requestBody =
        RequestBody.create(MediaType.parse("application/octet-stream"), new File(fileName));

    Request request = new Request.Builder().url("http://ledwayazure.cloudapp.net/ma/ledwayocr.aspx")
        .addHeader("UserName", mMyTaxNo)
        .addHeader("PassWord", "8887#@Ledway")
        .post(requestBody)
        .build();

    client.newCall(request).enqueue(new Callback() {
      @Override public void onFailure(Call call, IOException e) {
        orc.postValue(Resource.error(e.getMessage(), null));
      }

      @Override public void onResponse(Call call, Response response) throws IOException {
        try {
          String json1 = response.body().string();
          JSONObject jsonObject = new JSONObject(json1);
          if (jsonObject.getInt("returnCode") < 0 &&  jsonObject.has("returnInfo")) {
            orc.postValue(Resource.error(jsonObject.getString("returnInfo"), null));
          }
          JSONObject json = new JSONObject(jsonObject.getString("data"));
          int count = json.getInt("OCRCount");
          int limit = json.getInt("OCRLimit");
          String text = json.getString("OCRInfo");
          OCRData ocrData = new OCRData();
          ocrData.count = count;
          ocrData.limit = limit;
          ocrData.text = text;
          orc.postValue(Resource.success(ocrData));
        } catch (JSONException | IOException e) {
          e.printStackTrace();
          orc.postValue(Resource.error(e.getMessage(), null));
        }
      }
    });
  }

  private void takePictureFor(int typeIndex){
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      File photoFile = null;
      try {
        photoFile = new File(getImagePath(mTodoProd.prodNo,  pictureTypes[typeIndex], 1));
        mCurrentPhotoPath = photoFile.getAbsolutePath();
        photoFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (photoFile != null) {
        Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                BuildConfig.APPLICATION_ID + ".provider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        List<ResolveInfo> resolvedIntentActivities =
                getPackageManager().queryIntentActivities(takePictureIntent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
          String packageName = resolvedIntentInfo.activityInfo.packageName;

/*          grantUriPermission(packageName, photoURI,
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);*/
        }
        startActivityForResult(takePictureIntent,REQUEST_TAKE_FOR + typeIndex);
      }
    }
  }




  private  class MyViewHolder extends RecyclerView.ViewHolder{
    TextView txtLabel;
    MImageView imageView;
    int typeIndex;
    public MyViewHolder(@NonNull View itemView) {
      super(itemView);
      txtLabel = itemView.findViewById(R.id.txt_label);
      imageView = itemView.findViewById(R.id.image_view);
      imageView.setOnClickListener(v -> {
        File imageFile = new File(getImagePath(mTodoProd.prodNo,pictureTypes[typeIndex], 1));
        if(imageFile.exists()){
          Intent intent = new Intent();
          intent.setAction(Intent.ACTION_VIEW);
          intent.setDataAndType(FileProvider.getUriForFile(imageView.getContext(), imageView.getContext().getPackageName() + ".provider", imageFile), "image/*");
          intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          imageView.getContext().startActivity(intent);
        }
      });
      itemView.findViewById(R.id.txt_change).setOnClickListener(v -> {
        takePictureFor(typeIndex);
      });
    }
  }

  private class MyAdapter extends RecyclerView.Adapter<MyViewHolder>{


    public MyAdapter(){
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
      View itemView = layoutInflater.inflate(R.layout.list_item_image_view, parent, false);
      return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
      String pictureType = pictureTypes[position];
      holder.typeIndex = position;
      ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
      if (layoutParams instanceof FlexboxLayoutManager.LayoutParams){
        FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) layoutParams;
        if (pictureType.equals("Main")){
          flexboxLp.setFlexBasisPercent(1f);
        }else{
          flexboxLp.setFlexBasisPercent(0.48f);
        }
      }
      holder.txtLabel.setText(pictureType);
      if (pictureType.equals("Main")){
        Picasso.with(holder.imageView.getContext()).load(new File(mTodoProd.image1)).into(holder.imageView);
        holder.imageView.setImagePath(mTodoProd.image1);
      }else{
        String path = getImagePath(mTodoProd.prodNo,pictureType, 1);
        Picasso.with(holder.imageView.getContext()).load(new File(path)).into(holder.imageView);
        holder.imageView.setImagePath(path);
      }


    }

    @Override
    public int getItemCount() {
      return pictureTypes.length;
    }
  }
}
