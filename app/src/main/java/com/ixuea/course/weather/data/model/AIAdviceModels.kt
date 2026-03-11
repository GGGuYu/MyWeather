package com.ixuea.course.weather.data.model

/**
 * AI建议请求数据
 * 
 * @param currentWeather 当前天气数据
 * @param forecast 未来预报数据（取前12小时）
 */
data class AIAdviceRequest(
    val currentWeather: WeatherResponse,
    val forecast: List<Forecast>
)

/**
 * AI建议UI状态
 */
data class AIAdviceState(
    val advice: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * AI建议事件
 */
sealed class AIAdviceEvent {
    /**
     * 生成AI建议
     */
    object GenerateAdvice : AIAdviceEvent()

    /**
     * AI回复更新（流式）
     */
    data class AdviceUpdated(val content: String) : AIAdviceEvent()

    /**
     * AI回复完成
     */
    object AdviceCompleted : AIAdviceEvent()

    /**
     * 发生错误
     */
    data class Error(val message: String) : AIAdviceEvent()

    /**
     * 返回上一页
     */
    object NavigateBack : AIAdviceEvent()
}