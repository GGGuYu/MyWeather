package com.ixuea.course.weather.data.repository

import com.ixuea.course.weather.config.Config
import com.ixuea.course.weather.data.api.WeatherApiService

class WeatherRepository(private val apiService: WeatherApiService) {

    suspend fun getCurrentWeather(lat: Double, lon: Double) =
        apiService.getCurrentWeather(lat, lon, Config.API_KEY)


}