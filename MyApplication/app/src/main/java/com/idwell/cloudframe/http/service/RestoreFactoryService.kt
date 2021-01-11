package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.Data
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface RestoreFactoryService {
    @FormUrlEncoded
    @POST("device/restoreFactory")
    fun restoreFactory(@Field("device_id") device_id: Int, @Field("device_fcm_token") device_fcm_token: String): Observable<Base<Data>>
}
