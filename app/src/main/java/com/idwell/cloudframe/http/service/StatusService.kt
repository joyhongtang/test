package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.DeviceStatus

import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface StatusService {
    @FormUrlEncoded
    @POST("device/status")
    fun status(@Field("user_id") user_id: Int, @Field("device_id") device_id: Int, @Field("status") status: String): Observable<Base<DeviceStatus>>

    @FormUrlEncoded
    @POST("device/status")
    fun status(@Field("user_id") user_id: Int, @Field("device_id") device_id: Int, @Field("status") status: String, @Field("rename_name") rename_name: String): Observable<Base<DeviceStatus>>
}
