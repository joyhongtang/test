package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.City
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface GetCityListService {
    @GET("weather/getCityList")
    fun getCityList(@Query("cityName") cityName: String): Observable<Base<MutableList<City>>>
}
