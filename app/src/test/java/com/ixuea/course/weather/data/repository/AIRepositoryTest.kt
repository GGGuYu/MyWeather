package com.ixuea.course.weather.data.repository

import com.ixuea.course.weather.data.model.AIAdviceRequest
import com.ixuea.course.weather.data.model.Clouds
import com.ixuea.course.weather.data.model.Coord
import com.ixuea.course.weather.data.model.Forecast
import com.ixuea.course.weather.data.model.Main
import com.ixuea.course.weather.data.model.Sys
import com.ixuea.course.weather.data.model.Weather
import com.ixuea.course.weather.data.model.WeatherResponse
import com.ixuea.course.weather.data.model.Wind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * AIRepository 简单测试
 *
 * 注意：此测试需要有效的Moonshot API Key
 * 如果API Key无效或网络不通，测试会失败
 */
class AIRepositoryTest {

    @Test
    fun testGenerateAdvice(): Unit = runBlocking {
        // 创建测试用的天气数据
        val mockWeather = WeatherResponse(
            coord = Coord(lon = 116.4, lat = 39.9),
            weather = listOf(
                Weather(
                    id = 800,
                    main = "Clear",
                    description = "晴天",
                    icon = "01d"
                )
            ),
            main = Main(
                temp = 25.0,
                feelsLike = 26.0,
                temp_min = 20.0,
                temp_max = 28.0,
                pressure = 1013,
                humidity = 45
            ),
            visibility = 10000,
            wind = Wind(speed = 3.5, deg = 180),
            clouds = Clouds(all = 0),
            dt = 1710324000,
            sys = Sys(
                type = 2,
                id = 2004006,
                country = "CN",
                sunrise = 1710307200,
                sunset = 1710349200
            ),
            timezone = 28800,
            id = 1816670,
            name = "北京",
            cod = 200,
            list = null
        )

        val mockForecast = listOf(
            Forecast(
                dt = 1710330000,
                main = Main(
                    temp = 26.0,
                    feelsLike = 27.0,
                    temp_min = 25.0,
                    temp_max = 27.0,
                    pressure = 1012,
                    humidity = 50
                ),
                weather = listOf(
                    Weather(
                        id = 800,
                        main = "Clear",
                        description = "晴天",
                        icon = "01d"
                    )
                ),
                clouds = Clouds(all = 0),
                wind = Wind(speed = 3.0, deg = 180),
                visibility = 10000,
                dt_txt = "2024-03-13 15:00:00"
            )
        )

        val request = AIAdviceRequest(
            currentWeather = mockWeather,
            forecast = mockForecast
        )

        val repository = AIRepository()

        // 测试生成建议
        val advice = repository.generateAdvice(request).first()

        // 验证返回的建议不为空
        assert(advice.isNotEmpty()) {
            "AI应该返回非空的建议内容"
        }

        // 验证建议包含中文内容
        assert(advice.contains("穿衣") || advice.contains("建议") || advice.contains("天气")) {
            "建议内容应该包含相关关键词"
        }

        println("AI建议内容：")
        println(advice)
    }
}