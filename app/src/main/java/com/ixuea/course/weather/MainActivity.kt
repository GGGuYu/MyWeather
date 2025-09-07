package com.ixuea.course.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.location.LocationServices
import com.ixuea.course.weather.data.api.WeatherApiService
import com.ixuea.course.weather.data.location.DefaultLocationClient
import com.ixuea.course.weather.data.repository.WeatherRepository
import com.ixuea.course.weather.ui.WeatherScreen
import com.ixuea.course.weather.ui.WeatherViewModel
import com.ixuea.course.weather.ui.WeatherViewModelFactory
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        WeatherScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyWeatherTheme {
        Greeting("Android")
    }
}