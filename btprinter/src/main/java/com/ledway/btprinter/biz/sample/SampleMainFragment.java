package com.ledway.btprinter.biz.sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.DialogInterface;
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
import android.util.Log;
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
import com.ledway.btprinter.models.Resource;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.framework.FullScannerActivity;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static android.app.Activity.RESULT_OK;

/**
 * Created by togb on 2017/12/3.
 */

public class SampleMainFragment extends Fragment{

  private static final int RESULT_CAMERA_QR_CODE = 1;
  private static final int REQUEST_BUSINESS_CARD = 2;
  @BindView(R.id.edt_spec) EditText mEdtSpec;
  @BindView(R.id.img_business_card) ImageView mImgBusinssCard;
  @BindView(R.id.txt_hint_business_card) TextView mTxtHintBusinessCard;
  private Unbinder unbinder;
  private SampleMaster mSampleMaster;
  private String mCurrentPhotoPath;
  private MutableLiveData<Resource> uploading = new MutableLiveData<>();
  private ProgressDialog mProgressDialog;
  private CompositeSubscription mSubscription = new CompositeSubscription();
  public SampleMainFragment() {
     setHasOptionsMenu(true);
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    mSampleMaster =
        getActivity() != null ? ((SampleActivity) getActivity()).mSampleMaster
            : null;
    super.onCreate(savedInstanceState);

  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_sample_main, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    unbinder =ButterKnife.bind(this, view);
    if(mSampleMaster.image1 != null){
      Picasso.with(mImgBusinssCard.getContext()).load(new File(mSampleMaster.image1)).fit().into(mImgBusinssCard);
      mTxtHintBusinessCard.setVisibility(View.GONE);
    }
    if(mSampleMaster.desc != null){
      mEdtSpec.setText(mSampleMaster.desc);
    }
    initView();
    super.onViewCreated(view, savedInstanceState);

  }

  private void initView() {
    uploading.observe(this, resource -> {
      switch (resource.status){
        case LOADING:{
          showLoading();
          break;
        }
        case SUCCESS:{
          stopLoading();
          break;
        }
        case ERROR:{
          stopLoading();
          Toast.makeText(getContext(), resource.message, Toast.LENGTH_LONG).show();
        }
      }
    });
  }

  private void showLoading(){
   mProgressDialog =
        ProgressDialog.show(getActivity(), getString(R.string.upload), getString(R.string.wait_a_moment),
            false);
   mProgressDialog.setOnDismissListener(dialogInterface -> mProgressDialog = null);
  }

  private void stopLoading(){
    if(mProgressDialog != null){
      mProgressDialog.dismiss();
      mProgressDialog = null;
    }
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

  @OnClick(R.id.icon_scan_barcode) void  onBtnScanBarCode(){
    startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
        RESULT_CAMERA_QR_CODE);
  }

  @OnTextChanged(R.id.edt_spec) void onEdtSpecTextChanged(){
    if(mSampleMaster.desc == null || !mSampleMaster.desc.equals(mEdtSpec.getText().toString())){
      mSampleMaster.update_date = new Date();
    }
    mSampleMaster.desc = mEdtSpec.getText().toString();
  }
  @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode){
      case RESULT_CAMERA_QR_CODE:{
        if(RESULT_OK == resultCode){
          String qrcode = data.getStringExtra("barcode");
          mEdtSpec.setText(qrcode);
        }
        break;
      }
      case REQUEST_BUSINESS_CARD:{
        if (RESULT_OK ==  resultCode){
          File f = new File(mCurrentPhotoPath);
          if (f.exists()){
            if (f.length() < 1){
              f.delete();
            }
          }
          if (f.exists()){
            mSampleMaster.image1 = mCurrentPhotoPath;
            mSampleMaster.update_date = new Date();
            Picasso.with(mImgBusinssCard.getContext()).load(f).fit().into(mImgBusinssCard);
            mTxtHintBusinessCard.setVisibility(View.GONE);
          }
          //   upload();

        }
        break;
      }
    }

  }

  private void startTakePhoto(){
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
      File photoFile = null;
      try {
        photoFile =
            new File(
                MApp.getApplication().getPicPath() + "/" + getBusinessCardFileName() +".jpeg");
        mCurrentPhotoPath = photoFile.getAbsolutePath();
        photoFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (photoFile != null) {
        Uri photoURI =
            FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID+".fileprovider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        List<ResolveInfo> resolvedIntentActivities = getActivity().getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
          String packageName = resolvedIntentInfo.activityInfo.packageName;

          getActivity().grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        startActivityForResult(takePictureIntent, REQUEST_BUSINESS_CARD);
      }
    }
  }
  private String getBusinessCardFileName(){
    return mSampleMaster.guid.replaceAll("[\\*\\/\\\\\\?]","_");
  }

  @OnClick(R.id.img_business_card) void onImgBusinessCardClick(){
    startTakePhoto();
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
    }
    return super.onOptionsItemSelected(item);
  }


  private void uploadAll() {
    mSubscription.add(Observable.concat(Observable.just(mSampleMaster), Observable.defer(() -> {
      List<SampleMaster> data = new Select().from(SampleMaster.class)
          .where("isDirty =?", true)
          .orderBy(" create_date desc ")
          .execute();
      return Observable.from(data);
    }))
        .filter(sampleMaster -> !sampleMaster.isUploaded())
        .doOnSubscribe(() ->uploading.postValue(Resource.loading(null)))
        .flatMap(sampleMaster -> sampleMaster.remoteSave())
        .subscribeOn(Schedulers.io())
        .subscribe(new Subscriber<SampleMaster>() {
          @Override public void onCompleted() {
            uploading.postValue(Resource.success(null));
          }

          @Override public void onError(Throwable e) {
            uploading.postValue(Resource.error(e.getMessage(), null));
          }

          @Override public void onNext(SampleMaster sampleMaster) {

          }
        }));
  }
}
