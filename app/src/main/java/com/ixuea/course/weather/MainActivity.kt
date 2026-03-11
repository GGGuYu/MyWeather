package com.ixuea.course.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.ixuea.course.weather.data.api.WeatherApiService
import com.ixuea.course.weather.data.location.DefaultLocationClient
import com.ixuea.course.weather.data.repository.WeatherRepository
import com.ixuea.course.weather.ui.WeatherViewModel
import com.ixuea.course.weather.ui.WeatherViewModelFactory
import com.ixuea.course.weather.ui.navigation.AppNavigation
import com.ixuea.course.weather.ui.theme.MyWeatherTheme

class MainActivity : ComponentActivity() {
    private val locationClient by lazy {
        DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(this)
        )
    }
    private val apiService by lazy { WeatherApiService.create() }
    private val repository by lazy { WeatherRepository(apiService) }

    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(repository, locationClient)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyWeatherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val weatherState by viewModel.weatherState.collectAsState()

                    AppNavigation(
                        navController = navController,
                        weatherViewModel = viewModel,
                        currentWeather = weatherState.currentWeather,
                        forecast = weatherState.forecast
                    )
                }
            }
        }
    }
}