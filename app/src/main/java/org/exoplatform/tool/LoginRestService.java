package org.exoplatform.tool;

import org.exoplatform.model.PlatformInfo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

/**
 * Created by paristote on 3/7/16.
 */
public interface LoginRestService {

    @GET("/rest/private/platform/info")
    Call<PlatformInfo> login(@Header("Authorization") String authorization);


    @GET("/rest/private/platform/info")
    Call<PlatformInfo> login();

}
