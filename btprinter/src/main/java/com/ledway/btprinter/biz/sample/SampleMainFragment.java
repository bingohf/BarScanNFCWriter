package com.ledway.btprinter.biz.sample;

import android.app.Activity;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.Unbinder;
import com.ledway.btprinter.BuildConfig;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.framework.FullScannerActivity;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
    super.onViewCreated(view, savedInstanceState);

  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
  }

  @OnClick(R.id.icon_scan_barcode) void  onBtnScanBarCode(){
    startActivityForResult(new Intent(getActivity(), FullScannerActivity.class),
        RESULT_CAMERA_QR_CODE);
  }

  @OnTextChanged(R.id.edt_spec) void onEdtSpecTextChanged(){
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



}
