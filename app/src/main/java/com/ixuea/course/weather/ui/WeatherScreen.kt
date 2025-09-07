package com.ixuea.course.weather.ui


import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ixuea.course.weather.data.model.Forecast
import com.ixuea.course.weather.data.model.WeatherResponse
import com.ixuea.course.weather.utils.AQICalculator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val permissionState by viewModel.permissionState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        viewModel.onEvent(WeatherEvent.PermissionResult(perms))
    }

    LaunchedEffect(Unit) {
        if (permissionState.hasAnyLocationPermission()) {
            viewModel.onEvent(WeatherEvent.RequestLocation)
        } else {
            permissionLauncher.launch(locationPermissions)
        }
    }

    val context = LocalContext.current

    when {
        // 1. 权限未授予情况
        !permissionState.hasAnyLocationPermission() -> {
            PermissionRationaleView(
                shouldShowRationale = permissionState.shouldShowPermissionRationale,
                onRequestPermission = { permissionLauncher.launch(locationPermissions) },
                onOpenSettings = {
                    openAppSettings(context)
                }
            )
        }

        // 2. 加载状态
        locationState.isLoading || weatherState.isLoading
            -> {
            LoadingScreen()
        }

        // 3. 错误状态
        locationState.error != null -> {
            ErrorScreen(
                message = locationState.error!!,
                onRetry = { viewModel.onEvent(WeatherEvent.RequestLocation) }
            )
        }

        weatherState.error != null -> {
            ErrorScreen(
                message = weatherState.error!!,
                onRetry = {
                    viewModel.onEvent(WeatherEvent.RefreshData)
                }
            )
        }

        // 4. 正常显示天气数据
        weatherState.currentWeather != null -> {
            WeatherContent(
                weatherState = weatherState,
                onRefresh = {
                    viewModel.onEvent(WeatherEvent.RefreshData)
                }
            )
        }

        else -> {
            EmptyWeather()
        }
    }
}

/**
 * 天气
 */
@Composable
fun WeatherContent(weatherState: WeatherState, onRefresh: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 当前天气
        weatherState.currentWeather?.let { weather ->
            CurrentWeatherSection(weather, weatherState.aqiResult)
        }

        // 小时预报
        if (weatherState.forecast.isNotEmpty()) {
            HourlyForecastSection(
                forecasts = weatherState.forecast.take(24) // 只显示24小时内的预报
            )
        }

    }
}


@Composable
fun HourlyForecastSection(forecasts: List<Forecast>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "每小时预报",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(forecasts) { forecast ->
                HourlyForecastItem(forecast)
            }
        }
    }
}

@Composable
fun HourlyForecastItem(forecast: Forecast) {
    // 时间格式化（HH:mm 和 M/d）
    val (timeStr, dateStr) = remember(forecast.dt) {
        val dateTime = Instant.ofEpochSecond(forecast.dt)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

        Pair(
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            dateTime.format(DateTimeFormatter.ofPattern("M/d"))
        )
    }

    // 天气描述中文化
    val weatherDesc = remember(forecast.weather) {
        when (forecast.weather.first().main.lowercase(Locale.CHINA)) {
            "clear" -> "晴"
            "clouds" -> "多云"
            "rain" -> "雨"
            else -> forecast.weather.first().description
        }
    }


    Card(
        modifier = Modifier
            .width(100.dp)
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp) // 控制垂直间距
        ) {
            // 时间（顶部）
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )


            Spacer(modifier = Modifier.height(5.dp))

            AsyncImage(
                model = "https://openweathermap.org/img/wn/${forecast.weather.first().icon}@2x.png",
                contentDescription = weatherDesc,
                modifier = Modifier.size(36.dp)
            )

            // 天气描述（紧贴图标下方）
            Text(
                text = weatherDesc,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            // 温度（底部）
            Text(
                text = "${forecast.main.temp.toInt()}°C",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


/**
 * 当前天气
 */
@Composable
fun CurrentWeatherSection(
    weather: WeatherResponse,
    aqiResult: AQICalculator.AQIResult?
) {
    val weatherDesc = remember(weather.weather) {
        when (weather.weather.first().main.lowercase(Locale.CHINA)) {
            "clear" -> "晴"
            "clouds" -> "多云"
            "rain" -> "雨"
            else -> weather.weather.first().description
        }
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 30.dp,
                    bottom = 30.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            // 城市名称
            Text(
                text = "${weather.name} · ${weather.sys.country}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // 水平排列图标、温度、描述
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 天气图标（大尺寸）
                AsyncImage(
                    model = "https://openweathermap.org/img/wn/${weather.weather.first().icon}@4x.png",
                    contentDescription = weatherDesc,
                    modifier = Modifier.size(50.dp)
                )

                // 温度数值（突出显示）
                Text(
                    text = "${weather.main.temp.toInt()}°C",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 48.sp
                )

                // 天气描述
                Text(
                    text = weatherDesc,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            //空气指数
            aqiResult?.let {
                AirQualityBadge(it)
            }

            // 天气指标
            WeatherDetailsRow(
                feelsLike = weather.main.feelsLike.toInt(),
                humidity = weather.main.humidity,
                windSpeed = weather.wind.speed,
                pressure = weather.main.pressure
            )
        }
    }
}

@Composable
fun AirQualityBadge(aqiResult: AQICalculator.AQIResult) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Surface(
            color = aqiResult.bgColor.copy(alpha = 0.2f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = "空气${aqiResult.level} ${aqiResult.aqi}",
                style = MaterialTheme.typography.labelMedium,
                color = aqiResult.bgColor,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Text(
            text = aqiResult.advice,
            style = MaterialTheme.typography.labelSmall,
            color = aqiResult.bgColor,
        )
    }
}

@Composable
fun WeatherDetailsRow(feelsLike: Int, humidity: Int, windSpeed: Double, pressure: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        WeatherDetailItem(
            icon = Icons.Default.Thermostat,
            value = "$feelsLike°C",
            label = "体感"
        )

        WeatherDetailItem(
            icon = Icons.Default.Opacity,  // 使用湿度图标
            value = "$humidity%",
            label = "湿度"
        )

        WeatherDetailItem(
            icon = Icons.Rounded.Air,       // 使用空气流动图标
            value = "${windSpeed.toInt()} km/h",
            label = "风速"
        )

        WeatherDetailItem(
            icon = Icons.Rounded.Speed,     // 使用气压/速度图标
            value = "$pressure hPa",
            label = "气压"
        )
    }
}

@Composable
fun WeatherDetailItem(icon: ImageVector, value: String, label: String) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}


@Composable
fun ErrorScreen(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message ?: "Unknown error occurred",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }

    context.startActivity(intent)
}

@Composable
fun EmptyWeather(): Unit {
    Text("未知状态")
}

@Composable
fun PermissionRationaleView(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
): Unit {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (shouldShowRationale) {
                "需要位置权限来获取精准天气信息"
            } else {
                "请允许位置权限才能使用"
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        if (shouldShowRationale) {
            Text(
                text = "我们仅使用您的位置信息获取当地天气，不会存储或共享您的定位数据",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onRequestPermission) {
            Text("授予权限")
        }

        if (shouldShowRationale) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onOpenSettings) {
                Text("去设置中手动开启")
            }
        }

    }
}

