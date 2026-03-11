package com.ixuea.course.weather.ui.navigation

/**
 * 应用路由枚举
 */
sealed class Routes(val route: String) {
    /**
     * 天气主页面
     */
    object Weather : Routes("weather")

    /**
     * AI建议页面
     */
    object AIAdvice : Routes("ai_advice")
}