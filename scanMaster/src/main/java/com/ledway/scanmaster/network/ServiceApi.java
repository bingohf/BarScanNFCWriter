package com.ledway.scanmaster.network;

import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by togb on 2018/1/7.
 */

public interface ServiceApi {
  @PUT("group/{guid}") Observable<GroupResponse> getGroup(@Path("guid") String guid, @Body GroupRequest request);

  @POST("Sp/sp_getBill") Observable<SpResponse> sp_getBill(@Body
      Sp_getBill_Request request);

  @POST("Sp/sp_getDetail") Observable<SpResponse> sp_UpSampleDetail(@Body
      Sp_getDetail_Request request);

  @POST("Sp/Sp_GetScanMasterMenu") Observable<SpResponse> spGetScanMasterMenu(@Body
      SpGetMenuRequest request);
}
