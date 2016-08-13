package com.ledway.btprinter.network;

import retrofit2.http.GET;
import rx.Observable;

/**
 * Created by togb on 2016/8/7.
 */
public interface LedwayService {
  @GET("apkversion.txt")
  Observable<ApkVersionResponse> get_apk_version();
}
