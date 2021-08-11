package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.Signin

import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SignInService {
    @FormUrlEncoded
    @POST("device/signin")
    fun signin(@Field("device_token") device_token: String,
               @Field("device_fcm_token") device_fcm_token: String,
               @Field("companyName") companyName: String): Observable<Base<Signin>>
}