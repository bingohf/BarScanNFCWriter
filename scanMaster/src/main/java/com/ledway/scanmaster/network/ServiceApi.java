package com.ledway.scanmaster.network;

import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by togb on 2018/1/7.
 */

public interface ServiceApi {
  @PUT("group/{guid}") Observable<GroupResponse> getGroup(@Path("guid") String guid, @Body GroupRequest request);
}
