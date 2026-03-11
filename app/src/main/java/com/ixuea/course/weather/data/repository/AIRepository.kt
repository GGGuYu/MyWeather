package com.ixuea.course.weather.data.repository

import android.util.Log
import com.ixuea.course.BuildConfig
import com.ixuea.course.weather.data.model.AIAdviceRequest
import com.ixuea.course.weather.data.model.Forecast
import com.ixuea.course.weather.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * AIRepository - AI建议数据仓库
 *
 * 【职责】
 * 1. 封装Moonshot API的调用
 * 2. 将天气数据转换为AI Prompt
 * 3. 使用HTTP方式获取AI回复
 * 4. 提供统一的接口给ViewModel
 */
class AIRepository {

    companion object {
        const val TAG = "AIRepository"
        const val MOONSHOT_API_URL = "https://api.moonshot.cn/v1/chat/completions"
        const val MODEL = "moonshot-v1-8k"
    }

    /**
     * 生成AI穿衣建议
     *
     * @param request 包含当前天气和未来预报的请求数据
     * @return Flow<String> 返回AI回复
     */
    fun generateAdvice(request: AIAdviceRequest): Flow<String> = flow {
        val prompt = buildPrompt(request)
        Log.d(TAG, "Prompt: $prompt")

        val requestBody = buildRequestBody(prompt)

        // 使用HttpURLConnection发送请求
        val url = URL(MOONSHOT_API_URL)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer ${BuildConfig.MOONSHOT_API_KEY}")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
            }

            // 写入请求体
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = StringBuilder()
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                }

                val responseBody = response.toString()
                Log.d(TAG, "Response: $responseBody")

                val content = parseResponse(responseBody)
                if (content != null) {
                    emit(content)
                } else {
                    throw Exception("Failed to parse AI response")
                }
            } else {
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "API Error: $errorResponse")
                throw Exception("API request failed: $responseCode - $errorResponse")
            }
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 解析响应
     */
    private fun parseResponse(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                val choice = choices.getJSONObject(0)
                val message = choice.getJSONObject("message")
                message.getString("content")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse response error", e)
            null
        }
    }

    /**
     * 构建Prompt
     *
     * 包含：当前天气、未来12小时预报
     */
    private fun buildPrompt(request: AIAdviceRequest): String {
        val weather = request.currentWeather
        val forecast = request.forecast.take(12) // 取前12小时

        val currentInfo = """
            当前天气：
            - 地点：${weather.name}, ${weather.sys.country}
            - 温度：${weather.main.temp.toInt()}°C
            - 体感温度：${weather.main.feelsLike.toInt()}°C
            - 天气状况：${weather.weather.firstOrNull()?.description ?: "未知"}
            - 湿度：${weather.main.humidity}%
            - 风速：${weather.wind.speed} km/h
        """.trimIndent()

        val forecastInfo = if (forecast.isNotEmpty()) {
            val forecastText = forecast.joinToString("\n") { f ->
                "  - ${formatHour(f.dt_txt)}: ${f.main.temp.toInt()}°C, ${f.weather.firstOrNull()?.description ?: "未知"}"
            }
            "\n\n未来12小时预报：\n$forecastText"
        } else {
            ""
        }

        return """
            你是一位专业的穿衣顾问，请根据以下天气信息给出穿衣和出门建议：

            $currentInfo
            $forecastInfo

            请提供以下建议：
            1. **穿衣建议**：根据当前温度和未来趋势，建议穿什么衣服（包括上衣、裤子/裙子、鞋子）
            2. **出门准备**：是否需要带伞、带外套、防晒等
            3. **活动建议**：这种天气适合做什么户外活动或室内活动
            4. **特别提醒**：如果有大风、降雨、温差大等特殊情况请特别说明

            请用简洁友好的中文回复,尽可能简洁。
        """.trimIndent()
    }

    /**
     * 构建请求体
     */
    private fun buildRequestBody(prompt: String): String {
        val json = JSONObject()
        json.put("model", MODEL)
        json.put("temperature", 0.7)

        val messages = JSONArray()
        val systemMessage = JSONObject()
        systemMessage.put("role", "system")
        systemMessage.put("content", "你是一位专业的穿衣顾问，擅长根据天气给出实用的穿衣和出行建议。")
        messages.put(systemMessage)

        val userMessage = JSONObject()
        userMessage.put("role", "user")
        userMessage.put("content", prompt)
        messages.put(userMessage)

        json.put("messages", messages)

        return json.toString()
    }

    /**
     * 格式化时间字符串
     */
    private fun formatHour(dtTxt: String): String {
        // dt_txt格式: "2024-03-15 12:00:00"
        return dtTxt.split(" ").getOrNull(1)?.substring(0, 5) ?: dtTxt
    }
}