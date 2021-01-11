package com.idwell.cloudframe.http.service

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface MessageService {
    @Streaming
    @GET
    fun download(@Url url: String): Call<ResponseBody>
}
