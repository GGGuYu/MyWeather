package com.ixuea.course.weather.utils

import androidx.compose.ui.graphics.Color
import com.ixuea.course.weather.data.model.PollutionComponents

object AQICalculator {

    data class AQIResult(
        val aqi: Int,
        val level: String,            // 等级（一级、二级...）
        val description: String,      // 描述（优、良...）
        val primaryPollutant: String, // 主污染物
        val advice: String,            // 健康建议
        val bgColor: Color,
    )

    private data class IaqiRange(val cLow: Double, val cHigh: Double, val iLow: Int, val iHigh: Int)

    fun calculateAQIFromComponents(components: PollutionComponents): AQIResult {
        val coMg = components.co / 1000.0  // μg/m³ 转换为 mg/m³

        val pollutantIAQI = mapOf(
            "PM2.5" to calcIAQI(components.pm2_5, IAQI_BREAKPOINTS["PM2.5"]!!),
            "PM10" to calcIAQI(components.pm10, IAQI_BREAKPOINTS["PM10"]!!),
            "CO" to calcIAQI(coMg * 1000, IAQI_BREAKPOINTS["CO"]!!), // 转回 μg/m³ 计算
            "NO2" to calcIAQI(components.no2, IAQI_BREAKPOINTS["NO2"]!!),
            "SO2" to calcIAQI(components.so2, IAQI_BREAKPOINTS["SO2"]!!),
            "O3" to calcIAQI(components.o3, IAQI_BREAKPOINTS["O3"]!!)
        )

        val maxEntry = pollutantIAQI.maxByOrNull { it.value }!!

        val aqi = maxEntry.value
        val (level, description) = classifyAQI(aqi)
        val advice = getAdvice(aqi)

        return AQIResult(
            aqi = aqi,
            level = level,
            description = description,
            primaryPollutant = maxEntry.key,
            advice = advice,
            bgColor = getBackgroundColor(aqi)
        )
    }

    private fun calcIAQI(concentration: Double, breakpoints: List<IaqiRange>): Int {
        for (range in breakpoints) {
            if (concentration in range.cLow..range.cHigh) {
                return ((range.iHigh - range.iLow) * (concentration - range.cLow) /
                        (range.cHigh - range.cLow) + range.iLow).toInt()
            }
        }
        return 0
    }

    private fun getBackgroundColor(aqi: Int): Color = when (aqi) {
        in 0..50 -> Color(0xFF009966)     // 优 - 绿色
        in 51..100 -> Color(0xFFFFDE33)   // 良 - 黄色
        in 101..150 -> Color(0xFFFF9933)  // 轻度污染 - 橙色
        in 151..200 -> Color(0xFFCC0033)  // 中度污染 - 红色
        in 201..300 -> Color(0xFF660099)  // 重度污染 - 紫色
        else -> Color(0xFF7E0023)        // 严重污染 - 褐红色
    }

    private fun classifyAQI(aqi: Int): Pair<String, String> = when (aqi) {
        in 0..50 -> "一级" to "优"
        in 51..100 -> "二级" to "良"
        in 101..150 -> "三级" to "轻度污染"
        in 151..200 -> "四级" to "中度污染"
        in 201..300 -> "五级" to "重度污染"
        else -> "六级" to "严重污染"
    }

    private fun getAdvice(aqi: Int): String = when (aqi) {
        in 0..50 -> "空气质量令人满意，基本无空气污染。"
        in 51..100 -> "空气质量可接受，极少数特别敏感人群应减少户外活动。"
        in 101..150 -> "敏感人群应减少户外活动。"
        in 151..200 -> "儿童、老年人及心脏病、呼吸系统疾病患者应减少户外活动。"
        in 201..300 -> "儿童、老年人及患病人群应避免户外活动，一般人群减少活动。"
        else -> "所有人应尽量留在室内，避免体力消耗。"
    }

    private val IAQI_BREAKPOINTS = mapOf(
        "PM2.5" to listOf(
            IaqiRange(0.0, 35.0, 0, 50),
            IaqiRange(35.0, 75.0, 50, 100),
            IaqiRange(75.0, 115.0, 100, 150),
            IaqiRange(115.0, 150.0, 150, 200),
            IaqiRange(150.0, 250.0, 200, 300),
            IaqiRange(250.0, 350.0, 300, 400),
            IaqiRange(350.0, 500.0, 400, 500)
        ),
        "PM10" to listOf(
            IaqiRange(0.0, 50.0, 0, 50),
            IaqiRange(50.0, 150.0, 50, 100),
            IaqiRange(150.0, 250.0, 100, 150),
            IaqiRange(250.0, 350.0, 150, 200),
            IaqiRange(350.0, 420.0, 200, 300),
            IaqiRange(420.0, 500.0, 300, 400),
            IaqiRange(500.0, 600.0, 400, 500)
        ),
        "SO2" to listOf(
            IaqiRange(0.0, 50.0, 0, 50),
            IaqiRange(50.0, 150.0, 50, 100),
            IaqiRange(150.0, 475.0, 100, 150),
            IaqiRange(475.0, 800.0, 150, 200),
            IaqiRange(800.0, 1600.0, 200, 300),
            IaqiRange(1600.0, 2100.0, 300, 400),
            IaqiRange(2100.0, 2620.0, 400, 500)
        ),
        "NO2" to listOf(
            IaqiRange(0.0, 100.0, 0, 50),
            IaqiRange(100.0, 200.0, 50, 100),
            IaqiRange(200.0, 700.0, 100, 150),
            IaqiRange(700.0, 1200.0, 150, 200),
            IaqiRange(1200.0, 2340.0, 200, 300),
            IaqiRange(2340.0, 3090.0, 300, 400),
            IaqiRange(3090.0, 3840.0, 400, 500)
        ),
        "CO" to listOf(
            IaqiRange(0.0, 5000.0, 0, 50),
            IaqiRange(5000.0, 10000.0, 50, 100),
            IaqiRange(10000.0, 35000.0, 100, 150),
            IaqiRange(35000.0, 60000.0, 150, 200),
            IaqiRange(60000.0, 90000.0, 200, 300),
            IaqiRange(90000.0, 120000.0, 300, 400),
            IaqiRange(120000.0, 150000.0, 400, 500)
        ),
        "O3" to listOf(
            IaqiRange(0.0, 160.0, 0, 50),
            IaqiRange(160.0, 200.0, 50, 100),
            IaqiRange(200.0, 300.0, 100, 150),
            IaqiRange(300.0, 400.0, 150, 200),
            IaqiRange(400.0, 800.0, 200, 300),
            IaqiRange(800.0, 1000.0, 300, 400),
            IaqiRange(1000.0, 1200.0, 400, 500)
        )
    )
}