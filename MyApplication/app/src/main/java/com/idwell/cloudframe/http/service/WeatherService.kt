package com.idwell.cloudframe.http.service

import com.idwell.cloudframe.http.entity.Base
import com.idwell.cloudframe.http.entity.Forecast

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {

    @GET("weather/city_id")
    fun weather(
        @Query("city_id") city_id: String
    ): Observable<Base<Forecast>>

    @GET("weather/city_id")
    fun weather(
        @Query("city_id") city_id: String,
        @Query("time_zone") time_zone: String
    ): Observable<Base<Forecast>>

    @GET("weather/lat_lon")
    fun weather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("time_zone") time_zone: String
    ): Observable<Base<Forecast>>
}
