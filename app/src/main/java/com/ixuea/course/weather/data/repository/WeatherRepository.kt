package com.ixuea.course.weather.data.repository

import com.ixuea.course.weather.config.Config
import com.ixuea.course.weather.data.api.WeatherApiService
import com.ixuea.course.weather.data.model.AirQualityResponse

class WeatherRepository(private val apiService: WeatherApiService) {

    suspend fun getCurrentWeather(lat: Double, lon: Double) =
        apiService.getCurrentWeather(lat, lon, Config.API_KEY)

    suspend fun getAirQuality(lat: Double, lon: Double): AirQualityResponse {
        return apiService.getAirQuality(lat, lon)
    }

    suspend fun getWeatherForecast(lat: Double, lon: Double) =
        apiService.getWeatherForecast(lat, lon)
}