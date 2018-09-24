package com.ledway.btprinter.models;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Base64;
import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.ledway.btprinter.MApp;
import com.ledway.btprinter.R;
import com.ledway.btprinter.network.MyProjectApi;
import com.ledway.btprinter.network.model.RestSpResponse;
import com.ledway.btprinter.network.model.SpReturn;
import com.ledway.btprinter.network.model.Sp_UpSampleDetail_Request;
import com.ledway.btprinter.network.model.Sp_UpSampleDetail_Return;
import com.ledway.btprinter.network.model.Sp_UpSample_v3_Request;
import com.ledway.scanmaster.utils.IOUtil;
import com.ledway.scanmaster.utils.JsonUtils;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;

/**
 * Created by togb on 2016/5/29.
 */

@Table(name = "sample_master") public class SampleMaster extends Model implements Serializable {
  public List<SampleProdLink> sampleProdLinks = new ArrayList<>();

  @Column(name = "guid", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
  public String guid;

  @Column(name = "create_date") public Date create_date = new Date();

  @Column(name = "update_date") public Date update_date;

  @Column(name = "mac_address") public String mac_address;

  @Column(name = "line") public String line;
  @Column(name = "reader") public String reader;
  @Column(name = "qrcode") public String qrcode;
  @Column(name = "image1") public transient  String image1;
  @Column(name = "image2") public transient  String image2;
  @Column(name = "desc") public transient  String desc;
  @Column(name = "isDirty") public boolean isDirty = true;
  @Column(name ="ShareToDeviceId") public String shareToDeviceId;

  @Column(name ="dataFrom")
  public String dataFrom = "";

  private boolean mIsChanged = false;
  protected boolean isLoadedAll = false;
  public boolean isChanged(){
    return mIsChanged;
  }
  public void reset(){
    mIsChanged = false;
    isLoadedAll = false;
  }


  public SampleMaster() {
    super();
  }
  public String getImage1() {
    return image1;
  }

  public void setImage1(String image1) {
    mIsChanged = true;
    isDirty = true;
    this.image1 = image1;
  }
  public String getImage2() {
    return image2;
  }
  public void setImage2(String image2) {
    mIsChanged = true;
    isDirty = true;
    this.image2 = image2;
  }
  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {

    isDirty = true;
    mIsChanged = true;
    this.desc = desc;
  }

  public void setShareToDeviceId(String deviceId){
    isDirty = true;
    mIsChanged = true;
    this.shareToDeviceId = deviceId;
  }

  public void fetchLink(){
    From query =
        new Select(new String[] { "todo_prod.spec_desc,SampleProdLink.*" }).from(SampleProdLink.class)
            .join(TodoProd.class)
            .on("SampleProdLink.prod_id = todo_prod.prodno")
            .where("SampleProdLink.sample_id=?", guid);
    Cursor cursor = Cache.openDatabase().rawQuery(query.toSql(),query.getArguments());
    ArrayList<SampleProdLink> sampleProdLinks = new ArrayList<>();
    if(cursor.moveToFirst()){
      do{
        SampleProdLink sampleProdLink = new SampleProdLink();
        sampleProdLink.ext = cursor.getInt(cursor.getColumnIndex("ext"));
        sampleProdLink.create_date = new Date(cursor.getLong(cursor.getColumnIndex("create_date")));
        sampleProdLink.prod_id = cursor.getString(cursor.getColumnIndex("prod_id"));
        sampleProdLink.sample_id = cursor.getString(cursor.getColumnIndex("sample_id"));
        sampleProdLink.link_id = cursor.getString(cursor.getColumnIndex("link_id"));
        sampleProdLink.spec_desc = cursor.getString(cursor.getColumnIndex("spec_desc"));
        sampleProdLinks.add(sampleProdLink);
      }
      while (cursor.moveToNext());
    }
    this.sampleProdLinks = sampleProdLinks;
  }

  public List<SampleProdLink> items() {
    From query =
        new Select(new String[] { "todo_prod.spec_desc,SampleProdLink.*" }).from(SampleProdLink.class)
            .join(TodoProd.class)
            .on("SampleProdLink.prod_id = todo_prod.prodno")
            .where("SampleProdLink.sample_id=?", guid);
    Cursor cursor = Cache.openDatabase().rawQuery(query.toSql(),query.getArguments());
    ArrayList<SampleProdLink> sampleProdLinks = new ArrayList<>();
    if(cursor.moveToFirst()){
      do{
        SampleProdLink sampleProdLink = new SampleProdLink();
        sampleProdLink.ext = cursor.getInt(cursor.getColumnIndex("ext"));
        sampleProdLink.create_date = new Date(cursor.getLong(cursor.getColumnIndex("create_date")));
        sampleProdLink.prod_id = cursor.getString(cursor.getColumnIndex("prod_id"));
        sampleProdLink.sample_id = cursor.getString(cursor.getColumnIndex("sample_id"));
        sampleProdLink.link_id = cursor.getString(cursor.getColumnIndex("link_id"));
        sampleProdLink.spec_desc = cursor.getString(cursor.getColumnIndex("spec_desc"));
        sampleProdLinks.add(sampleProdLink);
      }
      while (cursor.moveToNext());
    }

    return sampleProdLinks;
  }

  public void allSave() {
    if (!isHasData()) {
      return;
    }
    if (TextUtils.isEmpty(guid)) {
      guid = MApp.getApplication().getSystemInfo().getDeviceId() + "_" + System.currentTimeMillis();
    }
    if (create_date == null) {
      create_date = new Date();
    }

    mac_address = MApp.getApplication().getSystemInfo().getDeviceId();
    update_date = new Date();
    qrcode = "http://vip.ledway.com.tw/i/s.aspx?series=" + guid;
    save();

    int i = 0;
    for (SampleProdLink sampleProdLink : sampleProdLinks) {
      sampleProdLink.ext = ++i;
      sampleProdLink.sample_id = guid;
      sampleProdLink.save();
    }
  }

  public void queryDetail() {
    reset();
    if (getId() != null) {
      List<SampleMaster> record = new Select().from(this.getClass()).where("id = ?", getId()).execute();
      SampleMaster temp = record.get(0);
      create_date = temp.create_date;
      update_date = temp.update_date;
      desc = temp.desc;
      image1 = temp.image1;
      image2 = temp.image2;
      line = temp.line;
      reader = temp.reader;
      qrcode = temp.qrcode;
      isDirty = temp.isDirty;
      guid = temp.guid;
      mac_address = temp.mac_address;
    }
    if (getId() != null) {
      sampleProdLinks = items();
    } else {
      sampleProdLinks = new ArrayList<>();
    }
    isLoadedAll = true;

  }

  public Iterator<SampleProdLink> prodIterator(){

    return sampleProdLinks.iterator();
  }

  public Observable<SampleProdLink>  addProd(final String prodno){
    return Observable.create(new Observable.OnSubscribe<SampleProdLink>() {
      @Override public void call(final Subscriber<? super SampleProdLink> subscriber) {
        if (isExist(prodno)){
          subscriber.onError(new Exception(MApp.getApplication().getString(R.string.prod_exists)));
        }else {
          isDirty = true;
          mIsChanged = true;
          final SampleProdLink sampleProdLink = new SampleProdLink();
          sampleProdLink.link_id = guid +"_" + prodno;
          sampleProdLink.prod_id = prodno;
          sampleProdLink.sample_id = guid;
          sampleProdLink.ext = sampleProdLinks.size() + 1;
          sampleProdLink.create_date = new Date();
          TodoProd.getTodoProd(prodno).subscribe(new Subscriber<TodoProd>() {
            @Override public void onCompleted() {
              subscriber.onCompleted();
            }

            @Override public void onError(Throwable e) {
              subscriber.onError(e);
            }

            @Override public void onNext(TodoProd todoProd) {
              sampleProdLink.save();
              sampleProdLinks.add(sampleProdLink);
              subscriber.onNext(sampleProdLink);
              subscriber.onCompleted();
            }
          });

        }

      }
    });

  }

  private boolean isExist(String prodNo){
    for(SampleProdLink sampleProdLink : sampleProdLinks){
      if (sampleProdLink.prod_id.equals(prodNo)){
        return true;
      }
    }
    return false;
  }

  public Observable<SampleMaster> remoteSave() {
    if (!isLoadedAll){
      queryDetail();
    }
    allSave();
    byte[] imageBuffer = new byte[]{};
    try {
      if(!TextUtils.isEmpty(image1)) {
        imageBuffer = IOUtil.readFile(image1);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    final Sp_UpSample_v3_Request request =new Sp_UpSample_v3_Request();
    request.series = guid;
    request.empno = mac_address;
    request.custMemo = desc;
    request.shareToDeviceId = shareToDeviceId;
    request.json = toJson();
    request.custCardPic = Base64.encodeToString(imageBuffer, Base64.DEFAULT);
    Observable<SampleMaster> obMaster = MyProjectApi.getInstance()
        .getDbService()
        .sp_UpSample_v3(request)
        .flatMap(new Func1<RestSpResponse<SpReturn>, Observable<SampleMaster>>() {
          @Override
          public Observable<SampleMaster> call(RestSpResponse<SpReturn> spReturnRestSpResponse) {
            SpReturn spReturn = spReturnRestSpResponse.result.get(0);
            int returnCode = spReturn.errCode;
            String returnMessage = spReturn.errData;
            if (returnCode == 1) {
              qrcode = "http://vip.ledway.com.tw/i/s.aspx?series=" + guid;
              save();
              return Observable.just(SampleMaster.this);
            } else {
              return Observable.error(new Exception(returnMessage));
            }
          }
        });



    Observable<SampleMaster> observableDetail =
        Observable.from(sampleProdLinks).flatMap(new Func1<SampleProdLink, Observable<SampleMaster>>() {
          @Override public Observable<SampleMaster> call(final SampleProdLink sampleProdLink) {

            Sp_UpSampleDetail_Request detailRequest = new Sp_UpSampleDetail_Request();
            detailRequest.empno = mac_address;
            detailRequest.series = guid;
            detailRequest.prodno =sampleProdLink.prod_id;
            detailRequest.itemExt = String.valueOf(sampleProdLink.ext);
            detailRequest.pcsnum=1 ;
                return MyProjectApi.getInstance().getDbService().sp_UpSampleDetail(detailRequest)
                    .flatMap(new Func1<RestSpResponse<Sp_UpSampleDetail_Return>, Observable<SampleMaster>>() {
                      @Override public Observable<SampleMaster> call(
                          RestSpResponse<Sp_UpSampleDetail_Return> response) {
                        int returnCode = response.result.get(0).errCode;
                        String returnMessage = response.result.get(0).errData;
                        String outProdNo = response.result.get(0).outProdno;
                        if (returnCode == 1) {
                          return Observable.just(SampleMaster.this);
                        }else{
                          return Observable.error(new Exception(returnMessage));
                        }
                      }
                    });

          }
        });
    return obMaster.concatWith(observableDetail).doOnCompleted(new Action0() {
      @Override public void call() {
        isDirty = false;
        save();
      }
    });
  }

  public boolean isUploaded() {
    return !isDirty;
  }

  public boolean isHasData() {
    File file1 = null;
    if (!TextUtils.isEmpty(image1)) {
      file1 =new File(image1);
    }
    File file2 = null;
    if (!TextUtils.isEmpty(image2)) {
      file2 = new File(image2);
    }

    return !TextUtils.isEmpty(desc) || !TextUtils.isEmpty(shareToDeviceId)|| ( file1 != null && file1.length() > 0 ) || (file2 != null
        && file2.length() > 0) || (sampleProdLinks != null && sampleProdLinks.size() > 0) ;
  }


  private String toJson(){
    return JsonUtils.Companion.toJson(this);
  }


}
