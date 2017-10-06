package com.ledway.btprinter.network;

import com.ledway.btprinter.network.model.ProductAppGetReturn;
import com.ledway.btprinter.network.model.RestDataSetResponse;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by togb on 2017/10/6.
 */

public interface DBService {
  @GET("dataset/{dataset}")  Observable<RestDataSetResponse<ProductAppGetReturn>> query(
      @Path("dataset") String dataset, @Query("query") String query,
      @Query("orderBy") String orderBy);
}
