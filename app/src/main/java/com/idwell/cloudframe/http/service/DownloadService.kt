package com.idwell.cloudframe.http.service

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadService {
    @Headers("Accept-Encoding:identity")
    @Streaming
    @GET
    fun download(@Url url: String): Observable<ResponseBody>
}
