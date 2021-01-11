package com.idwell.cloudframe.http.service;

import com.idwell.cloudframe.http.entity.Base;

import com.idwell.cloudframe.http.entity.Version;
import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * author : chason
 * mailbox : 156874547@qq.com
 * time : 2018/1/18 19:01
 * version : 1.0
 * describe :
 */

public interface VersionService {
    @FormUrlEncoded
    @POST("device/version")
    Observable<Base<Version>> version(@Field("device_id") int device_id,
                                      @Field("version") int version);
}
