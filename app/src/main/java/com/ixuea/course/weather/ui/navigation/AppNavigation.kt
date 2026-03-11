package com.ixuea.course.weather.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ixuea.course.weather.data.model.Forecast
import com.ixuea.course.weather.data.model.WeatherResponse
import com.ixuea.course.weather.data.repository.AIRepository
import com.ixuea.course.weather.ui.AIScreen
import com.ixuea.course.weather.ui.AIViewModelFactory
import com.ixuea.course.weather.ui.WeatherScreen
import com.ixuea.course.weather.ui.WeatherViewModel

/**
 * 应用导航图
 *
 * @param navController 导航控制器
 * @param weatherViewModel 天气ViewModel
 * @param currentWeather 当前天气数据
 * @param forecast 天气预报数据
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    weatherViewModel: WeatherViewModel,
    currentWeather: WeatherResponse?,
    forecast: List<Forecast>
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Weather.route
    ) {
        // 天气主页面
        composable(Routes.Weather.route) {
            WeatherScreen(
                viewModel = weatherViewModel,
                onAIAdviceClick = {
                    navController.navigate(Routes.AIAdvice.route)
                }
            )
        }

        // AI建议页面
        composable(Routes.AIAdvice.route) {
            val aiRepository = AIRepository()
            val aiViewModel: com.ixuea.course.weather.ui.AIViewModel = viewModel(
                factory = AIViewModelFactory(
                    repository = aiRepository,
                    currentWeather = currentWeather,
                    forecast = forecast
                )
            )

            AIScreen(
                viewModel = aiViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}