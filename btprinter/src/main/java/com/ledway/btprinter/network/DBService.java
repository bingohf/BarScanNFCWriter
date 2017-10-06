package com.ledway.btprinter.network;

import com.ledway.btprinter.network.model.ProductAppGetReturn;
import com.ledway.btprinter.network.model.ProductReturn;
import com.ledway.btprinter.network.model.RestDataSetResponse;
import com.ledway.btprinter.network.model.RestSpResponse;
import com.ledway.btprinter.network.model.SpReturn;
import com.ledway.btprinter.network.model.Sp_UpSampleDetail_Request;
import com.ledway.btprinter.network.model.Sp_UpSampleDetail_Return;
import com.ledway.btprinter.network.model.Sp_UpSample_v3_Request;
import com.ledway.btprinter.network.model.TotalUserReturn;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by togb on 2017/10/6.
 */

public interface DBService {
  @GET("dataset/PRODUCTAPPGET")
  Observable<RestDataSetResponse<ProductAppGetReturn>> getProductAppGet(
      @Query("query") String query, @Query("orderBy") String orderBy);

  @GET("dataset/product") Observable<RestDataSetResponse<ProductReturn>> getProduct(
      @Query("query") String query, @Query("orderBy") String orderBy);

  @POST("Sp/sp_UpSample_v3") Observable<RestSpResponse<SpReturn>> sp_UpSample_v3(@Body
      Sp_UpSample_v3_Request request);

  @POST("Sp/sp_UpSampleDetail") Observable<RestSpResponse<Sp_UpSampleDetail_Return>> sp_UpSampleDetail(@Body
      Sp_UpSampleDetail_Request request);

  @GET("sql/{sql}") Observable<RestDataSetResponse<TotalUserReturn>> queryTotalUser(@Path("sql") String sql);



}
