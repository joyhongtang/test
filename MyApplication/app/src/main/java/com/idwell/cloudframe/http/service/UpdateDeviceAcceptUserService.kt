package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.Data
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface UpdateDeviceAcceptUserService {
    @FormUrlEncoded
    @POST("device/updateDeviceAcceptUser")
    fun updateDeviceAcceptUser(@Field("device_id") device_id: Int, @Field("ifAccept") ifAccept: String): Observable<Base<Data>>
}