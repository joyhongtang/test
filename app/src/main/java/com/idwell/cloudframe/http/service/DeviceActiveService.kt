package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.DeviceToken
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface DeviceActiveService {
    @FormUrlEncoded
    @POST("device/device_active")
    fun deviceActive(@Field("serial_number") serial_number: String, @Field("mac_address") mac_address: String): Observable<Base<DeviceToken>>
}