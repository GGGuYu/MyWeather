package com.ixuea.course.weather.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Long,
    val sys: Sys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int,
    val list: List<Forecast>? = null
)

data class Coord(
    val lon: Double,
    val lat: Double
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Main(
    val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int
)

data class Wind(
    val speed: Double,
    val deg: Int
)

data class Clouds(
    val all: Int
)

data class Sys(
    val type: Int,
    val id: Int,
    val country: String,
    val sunrise: Long,
    val sunset: Long
)

data class Forecast(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int,
    val dt_txt: String
)

//空气相关
data class AirQualityResponse(
    val coord: Coord,
    val list: List<AirQualityData>
)

data class AirQualityData(
    val main: AirQualityMain,
    val components: PollutionComponents,
    val dt: Long
)

data class AirQualityMain(
    val aqi: Int // 1-5级别
)

data class PollutionComponents(
    val co: Double,    // 一氧化碳
    val no2: Double,   // 二氧化氮
    val o3: Double,    // 臭氧
    val so2: Double,   // 二氧化硫
    val pm2_5: Double, // PM2.5
    val pm10: Double   // PM10
)