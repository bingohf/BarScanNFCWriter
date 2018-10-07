package com.ledway.btprinter.biz.sample;

import android.app.ProgressDialog;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import com.activeandroid.query.Select;
import com.ledway.btprinter.BuildConfig;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import com.ledway.btprinter.models.TodoProd;
import com.ledway.btprinter.network.model.RestSpResponse;
import com.ledway.btprinter.network.model.SpReturn;
import com.ledway.framework.FullScannerActivity;
import com.ledway.scanmaster.model.OCRData;
import com.ledway.scanmaster.model.Resource;
import com.ledway.scanmaster.utils.BizUtils;
import com.ledway.scanmaster.utils.ContextUtils;
import com.ledway.scanmaster.utils.IOUtil;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;
import static com.activeandroid.Cache.getContext;

/**
 * Created by togb on 2017/12/3.
 */

public class SampleMainFragment extends Fragment {

  private static final int RESULT_CAMERA_QR_CODE = 1;
  private static final int REQUEST_BUSINESS_CARD = 2;
  private static final int REQUEST_CAMERA_SHARE_TO = 3;
  @BindView(R.id.edt_spec) EditText mEdtSpec;
  @BindView(R.id.img_business_card) ImageView mImgBusinssCard;
  @BindView(R.id.txt_hint_business_card) TextView mTxtHintBusinessCard;
  @BindView(R.id.btn_ocr) View mOCRVIew;
  private Unbinder unbinder;
  private SampleMaster mSampleMaster;
  private String mCurrentPhotoPath;
  private MutableLiveData<Resource> uploading = new MutableLiveData<>();
  private ProgressDialog mProgressDialog;
  private CompositeSubscription mSubscription = new CompositeSubscription();
  private MutableLiveData<Resource<OCRData>> orc = new MutableLiveData<>();
  private String mMyTaxNo;

  public SampleMainFragment() {
    setHasOptionsMenu(true);
  }

  @OnClick(R.id.icon_scan_barcode) void onBtnScanBarCode() {
    startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
        RESULT_CAMERA_QR_CODE);
  }

  @OnTextChanged(R.id.edt_spec) void onEdtSpecTextChanged() {
    if (mSampleMaster.desc == null || !mSampleMaster.desc.equals(mEdtSpec.getText().toString())) {
      mSampleMaster.update_date = new Date();
      mSampleMaster.isDirty = true;
    }
    mSampleMaster.desc = mEdtSpec.getText().toString();
  }

  @OnClick(R.id.txt_hint_business_card) void onBtnHintBusinessCardClick() {
    startTakePhoto();
  }

  private void startTakePhoto() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
      File photoFile = null;
      try {
        photoFile = new File(
            MApp.getApplication().getPicPath() + "/" + getBusinessCardFileName() + ".jpeg");
        mCurrentPhotoPath = photoFile.getAbsolutePath();
        photoFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (photoFile != null) {
        Uri photoURI =
            FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider",
                photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        List<ResolveInfo> resolvedIntentActivities = getActivity().getPackageManager()
            .queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
          String packageName = resolvedIntentInfo.activityInfo.packageName;

/*          getActivity().grantUriPermission(packageName, photoURI,
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);*/
        }
        startActivityForResult(takePictureIntent, REQUEST_BUSINESS_CARD);

      }
    }
  }

  private String getBusinessCardFileName() {
    return mSampleMaster.guid.replaceAll("[\\*\\/\\\\\\?]", "_");
  }

  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case RESULT_CAMERA_QR_CODE: {
        if (RESULT_OK == resultCode) {
          String qrcode = data.getStringExtra("barcode");
          mEdtSpec.setText(qrcode);
        }
        break;
      }
      case REQUEST_BUSINESS_CARD: {
        if (RESULT_OK == resultCode) {
          File f = new File(mCurrentPhotoPath);
          if (f.exists()) {
            if (f.length() < 1) {
              f.delete();
            }
          }
          if (f.exists()) {
            IOUtil.cropImage(f);
            mSampleMaster.image1 = mCurrentPhotoPath;
            mSampleMaster.update_date = new Date();
            Picasso.with(mImgBusinssCard.getContext()).invalidate(f);
            Picasso.with(mImgBusinssCard.getContext()).load(f).into(mImgBusinssCard);
            mSampleMaster.isDirty = true;
            mTxtHintBusinessCard.setVisibility(View.GONE);
          }
          //   upload();

        }
        break;
      }
      case REQUEST_CAMERA_SHARE_TO: {
        if (RESULT_OK == resultCode) {
          String qrcode = data.getStringExtra("barcode");
          mSampleMaster.shareToDeviceId = qrcode;
          mSampleMaster.update_date = new Date();
          Toast.makeText(getActivity(), qrcode, Toast.LENGTH_LONG).show();
          if (mSampleMaster.isHasData()) {
            uploadAll();
          } else {
            Toast.makeText(getActivity(), R.string.pls_input_data, Toast.LENGTH_LONG).show();
          }
        }

        break;
      }
    }
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    mSampleMaster = getActivity() != null ? ((SampleActivity) getActivity()).mSampleMaster : null;
    super.onCreate(savedInstanceState);
    initView();
    mMyTaxNo = BizUtils.getMyTaxNo(getContext());
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_sample_main, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    unbinder = ButterKnife.bind(this, view);
    if (mSampleMaster.image1 != null) {
      Picasso.with(mImgBusinssCard.getContext())
          .load(new File(mSampleMaster.image1))
          .into(mImgBusinssCard);
      mTxtHintBusinessCard.setVisibility(View.GONE);
    }
    if (mSampleMaster.desc != null) {
      mEdtSpec.setText(mSampleMaster.desc);
    }
    super.onViewCreated(view, savedInstanceState);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
    stopLoading();
  }

  @Override public void onDestroy() {
    super.onDestroy();
    mSubscription.clear();
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.sample_main_menu, menu);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_upload: {
        if (mSampleMaster.isHasData()) {
          uploadAll();
        } else {
          Toast.makeText(getActivity(), R.string.pls_input_data, Toast.LENGTH_LONG).show();
        }
        break;
      }
      case R.id.action_print_preview: {
        if (mSampleMaster.isHasData()) {
          mSampleMaster.allSave();
          //  startActivity(new Intent(this, PrintPreviewActivity.class));
        } else {
          Toast.makeText(getActivity(), R.string.pls_input_data, Toast.LENGTH_LONG).show();
        }
        break;
      }
      case R.id.action_share_to: {
        Toast.makeText(getActivity(), R.string.help_share_to, Toast.LENGTH_LONG).show();
        startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
            REQUEST_CAMERA_SHARE_TO);
        break;
      }
      case R.id.action_re_take_photo: {
        startTakePhoto();
        break;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  private void initView() {
    uploading.observe(this, resource -> {
      switch (resource.status) {
        case LOADING: {
          showLoading();
          break;
        }
        case SUCCESS: {
          if(resource.data instanceof String){
            Toast.makeText(getContext(), (String)resource.data, Toast.LENGTH_LONG).show();
          }
          stopLoading();
          break;
        }
        case ERROR: {
          stopLoading();
          Toast.makeText(getContext(), resource.message, Toast.LENGTH_LONG).show();
        }
      }
    });
    orc.observe(this, ocrData -> {
      switch (ocrData.status) {
        case LOADING: {
          showLoading();
          break;
        }
        case ERROR: {
          stopLoading();
          Toast.makeText(getContext(), ocrData.message, Toast.LENGTH_LONG).show();
          break;
        }
        case SUCCESS: {
          mEdtSpec.setText(ocrData.data.text);
          if(ocrData.data.limit - ocrData.data.count  <=100) {
            Toast.makeText(getContext(),
                getString(R.string.ocr_count_limit, ocrData.data.count, ocrData.data.limit), Toast.LENGTH_LONG).show();
          }
          stopLoading();
          break;
        }
      }
    });
  }

  private void showLoading() {
    mProgressDialog = ProgressDialog.show(getActivity(), getString(R.string.upload),
        getString(R.string.wait_a_moment), false);
    mProgressDialog.setOnDismissListener(dialogInterface -> mProgressDialog = null);
  }

  private void stopLoading() {
    if (mProgressDialog != null) {
      mProgressDialog.dismiss();
      mProgressDialog = null;
    }
  }

  private void uploadAll() {
    mSampleMaster.isDirty = true;

    mSubscription.add(uploadSample().mergeWith(uploadProduct())

        .subscribe(new Subscriber<String>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            Timber.e(e);
            uploading.postValue(Resource.error(ContextUtils.getMessage(e), null));
          }

          @Override public void onNext(String message) {
            uploading.postValue(Resource.success(message));
          }
        }));
  }

  private Observable<String> uploadProduct() {
    StringBuilder sb = new StringBuilder();
    sb.append(" 1 > 2");
    ArrayList<String> ids = new ArrayList<>();
    for (SampleProdLink item : mSampleMaster.sampleProdLinks) {
      sb.append(" or ");
      ids.add(item.prodNo);
      sb.append(" prodNo =?");
    }
    String where =
        "( " + sb.toString() + ") and (uploaded_time is NULL  or update_time > uploaded_time)";
    String[] args = ids.toArray(new String[ids.size()]);
    return Observable.just(new Pair<>(where, args))
        .map((Func1<Pair<String, String[]>, List<TodoProd>>) stringPair -> new Select().from(
            TodoProd.class).where(stringPair.first, stringPair.second).execute())
        .flatMap(Observable::from)
        .flatMap(todoProd -> todoProd.remoteSave2()
            .flatMap(spReturnRestSpResponse -> {
          int returnCode = spReturnRestSpResponse.result.get(0).errCode;
          String returnMessage = spReturnRestSpResponse.result.get(0).errData;
          if (returnCode == 1) {
            todoProd.uploaded_time = new Date();
            todoProd.save();
          } else {
            return Observable.error(new Exception(returnMessage));
          }

          return Observable.just(spReturnRestSpResponse);
        })        .onErrorResumeNext(throwable -> Observable.empty()))
        .subscribeOn(Schedulers.io())
        .ignoreElements().cast(String.class);
  }

  private Observable<String> uploadSample() {
    return Observable.concat(Observable.just(mSampleMaster), Observable.defer(() -> {
      List<SampleMaster> data = new Select().from(SampleMaster.class)
          .where("isDirty =? and guid <>?", true, mSampleMaster.guid)
          .orderBy(" create_date desc ")
          .execute();
      return Observable.from(data);
    }))
        .filter(sampleMaster -> !sampleMaster.isUploaded())
        .doOnSubscribe(() -> uploading.postValue(Resource.loading(null)))
        .concatMap(sampleMaster -> sampleMaster.remoteSave())
        .subscribeOn(Schedulers.io());
  }

  private void parseOCR(StringBuilder sb, Object obj) throws JSONException {
    if (obj instanceof JSONArray) {
      for (int i = 0; i < ((JSONArray) obj).length(); ++i) {
        JSONObject item = ((JSONArray) obj).getJSONObject(i);
        parseOCR(sb, item);
      }
    } else if (obj instanceof JSONObject) {
      if (((JSONObject) obj).has("text")) {
        String text = ((JSONObject) obj).getString("text");
        if (!TextUtils.isEmpty(text)) {
          sb.append(text + " ");
        }
      } else if (((JSONObject) obj).has("lines")) {
        JSONArray lines = ((JSONObject) obj).getJSONArray("lines");
        parseOCR(sb, lines);
      } else if (((JSONObject) obj).has("words")) {
        JSONArray words = ((JSONObject) obj).getJSONArray("words");
        parseOCR(sb, words);
      } else if (((JSONObject) obj).has("regions")) {
        JSONArray regions = ((JSONObject) obj).getJSONArray("regions");
        parseOCR(sb, regions);
      }
    }
  }

  @OnClick(R.id.img_business_card) void onImgBusinessCardClick() {
    if (mSampleMaster.image1 == null) {
      startTakePhoto();
    } else {
      Intent intent = new Intent();
      intent.setAction(Intent.ACTION_VIEW);
      intent.setDataAndType(
          FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider",
              new File(mSampleMaster.image1)), "image/*");
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      getContext().startActivity(intent);

    }
  }

  @OnClick(R.id.btn_ocr) void onBtnOCRClick() {
    if (!mEdtSpec.getText().toString().trim().isEmpty()) {
      Toast.makeText(getContext(), R.string.hint_clear_business_card_hint_first, Toast.LENGTH_SHORT)
          .show();
      return;
    }
    if (mSampleMaster.image1 == null) {
      Toast.makeText(getContext(), R.string.no_image, Toast.LENGTH_SHORT).show();
      return;
    }
    ocrImage(mSampleMaster.image1);
  }

  private void ocrImage(String fileName) {
    orc.setValue(Resource.loading(null));
    OkHttpClient client = new OkHttpClient();
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
          JSONObject jsonObject = new JSONObject(response.body().string());
          if(jsonObject.getInt("returnCode") <0){
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
        } catch (JSONException| IOException e) {
          e.printStackTrace();
          orc.postValue(Resource.error(e.getMessage(), null));
        }
      }
    });



    /*MultipartBody.Part part = MultipartBody.Part.create(requestBody);
    MyProjectApi.getInstance()
        .getDbService()
        .ocr(
            "http://116.238.76.101:20082/WebLabelPub2015/Ledwayocr.aspx",
            "application/octet-stream",
            "8887#@Ledway",
            mMyTaxNo,
            part)
        .subscribeOn(Schedulers.io())
        .map(responseBody -> {
          try {
            StringBuilder sb = new StringBuilder();
            JSONObject jsonObject = new JSONObject(responseBody.string());
            if(jsonObject.getInt("returnCode") <0){
              throw new RuntimeException(new Exception(jsonObject.getString("returnInfo")));
            }
            return new JSONObject(jsonObject.getString("data"));
          } catch (JSONException| IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        })
        .subscribe(new Subscriber<JSONObject>() {
          @Override public void onCompleted() {

          }

          @Override public void onError(Throwable e) {
            orc.postValue(Resource.error(e.getMessage(), null));
          }

          @Override public void onNext(JSONObject json) {
            try {
              int count = json.getInt("OCRCount");
              int limit = json.getInt("OCRLimit");
              String text = json.getString("OCRInfo");
              OCRData ocrData = new OCRData();
              ocrData.count = count;
              ocrData.limit = limit;
              ocrData.text = text;
              orc.postValue(Resource.success(ocrData));
            } catch (JSONException e) {
              e.printStackTrace();
              orc.postValue(Resource.error(e.getMessage(), null));
            }
          }
        });*/
  }
}
