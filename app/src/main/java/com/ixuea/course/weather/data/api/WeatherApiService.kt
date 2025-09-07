package com.ixuea.course.weather.data.api

import com.ixuea.course.weather.config.Config
import com.ixuea.course.weather.data.model.AirQualityResponse
import com.ixuea.course.weather.data.model.WeatherResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * 网络API
 */
interface WeatherApiService {

    /**
     * 获取该经纬度当前天气
     */
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("lang") lang: String = "zh_cn",
        @Query("units") units: String = "metric"
    ): WeatherResponse


    @GET("data/2.5/air_pollution")
    suspend fun getAirQuality(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String = Config.API_KEY
    ): AirQualityResponse


    companion object {
        const val BASE_URL = "https://api.openweathermap.org/"

        /**
         * 创建API Service
         */
        fun create(): WeatherApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .connectTimeout(10, TimeUnit.SECONDS) // 连接超时
                .readTimeout(15, TimeUnit.SECONDS)    // 读取超时
                .writeTimeout(15, TimeUnit.SECONDS)   // 写入超时
                .callTimeout(30, TimeUnit.SECONDS)    // 整个调用超时（包括重定向）
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApiService::class.java)
        }
    }
}