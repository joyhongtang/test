package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.AcceptBind
import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.Data
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AcceptBindService {
    @FormUrlEncoded
    @POST("device/acceptBind")
    fun acceptBind(@Field("user_id") user_id: Int, @Field("device_id") device_id: Int, @Field("acceptBind") acceptBind: String): Observable<Base<AcceptBind>>
}
