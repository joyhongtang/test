package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.DeviceUser

import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface DeviceUserService {
    @FormUrlEncoded
    @POST("device/device_user")
    fun deviceUser(@Field("device_id") device_id: Int): Observable<Base<DeviceUser>>
}
