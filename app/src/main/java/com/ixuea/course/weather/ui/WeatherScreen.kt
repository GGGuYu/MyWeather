package com.ixuea.course.weather.ui


// ================== Android 基础组件导入 ==================
// Manifest.permission - 用于定义 Android 权限常量
import android.Manifest
// Context - Android 上下文，用于访问系统资源和服务
import android.content.Context
// Intent - 用于在组件间传递消息（比如跳转到设置页面）
import android.content.Intent
// Uri - 统一资源标识符，用于构建跳转链接
import android.net.Uri
// Settings - Android 系统设置相关类
import android.provider.Settings

// ================== Jetpack Compose 权限相关 ==================
// rememberLauncherForActivityResult - Compose 中用于请求权限的启动器（替代传统的 onActivityResult）
import androidx.activity.compose.rememberLauncherForActivityResult
// ActivityResultContracts.RequestMultiplePermissions - 可以同时请求多个权限的契约
import androidx.activity.result.contract.ActivityResultContracts

// ================== Compose UI 组件导入 ==================
// foundation 包：基础布局功能
import androidx.compose.foundation.background
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

// material.icons - Material Design 图标库
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Speed

// material3 - Material Design 3 组件（最新版）
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

// compose.runtime - Compose 运行时（生命周期、状态管理）
// Composable - 标记这是一个 Compose 函数
import androidx.compose.runtime.Composable
// LaunchedEffect - 在 Compose 中执行副作用（比如启动时检查权限）
import androidx.compose.runtime.LaunchedEffect
// collectAsState - 将 Kotlin Flow 转换为 Compose 状态
import androidx.compose.runtime.collectAsState
// getValue - 用于解构状态
import androidx.compose.runtime.getValue
// remember - 记住计算结果，避免重复计算
import androidx.compose.runtime.remember

// compose.ui - UI 基础类
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
// LocalContext - 获取当前 Composable 的 Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Coil - 图片加载库（类似 Glide/Picasso）
import coil.compose.AsyncImage

// 项目内部导入
import com.ixuea.course.weather.data.model.Forecast
import com.ixuea.course.weather.data.model.Weather
import com.ixuea.course.weather.data.model.WeatherResponse
import com.ixuea.course.weather.utils.AQICalculator
import com.ixuea.course.weather.utils.DateFormatter

// Java 时间 API
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * WeatherScreen - 天气主屏幕 Composable 函数
 * 
 * 这是整个天气应用的入口 UI 组件
 * 负责：1. 处理权限申请 2. 根据状态显示不同界面 3. 协调 ViewModel 和 UI
 * 
 * @param viewModel - 传入的 ViewModel，管理所有业务逻辑和状态
 */
@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    // ================== 步骤1：观察 ViewModel 中的状态 ==================
    // collectAsState() - 将 StateFlow 转为 Compose 可观察的状态
    // "by" 关键字 - 使用委托，直接访问值（不用 .value）
    // 这3个状态任何一个变化，都会触发 UI 重新渲染
    
    // permissionState - 权限状态（是否已授权位置权限）
    val permissionState by viewModel.permissionState.collectAsState()
    // locationState - 定位状态（正在定位/定位成功/失败）
    val locationState by viewModel.locationState.collectAsState()
    // weatherState - 天气数据状态（加载中/成功/失败）
    val weatherState by viewModel.weatherState.collectAsState()

    // ================== 步骤2：定义要申请的权限数组 ==================
    // ACCESS_FINE_LOCATION - 精确定位（GPS，精度高，耗电大）
    // ACCESS_COARSE_LOCATION - 粗略定位（网络/WiFi，精度低，省电）
    // 同时申请两个，系统会让用户选择一种
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    // ================== 步骤3：创建权限请求启动器 ==================
    // rememberLauncherForActivityResult - Compose 中专用的 ActivityResult API
    // 替代了传统 Android 的 startActivityForResult + onActivityResult
    // 
    // ActivityResultContracts.RequestMultiplePermissions - 请求多个权限的契约类
    // 用户选择后，回调函数会收到 Map<String, Boolean>（权限名 -> 是否授权）
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        // 权限请求完成后，通过事件通知 ViewModel
        // 将结果包装成 WeatherEvent.PermissionResult 事件
        viewModel.onEvent(WeatherEvent.PermissionResult(perms))
    }

    // ================== 步骤4：页面启动时的副作用 ==================
    // LaunchedEffect(Unit) - 在 Composable 进入组合（显示）时执行一次
    // Unit 作为 key，意味着只在首次显示时执行，后续重组不会重复执行
    // 这是 Compose 中处理副作用的标准方式
    LaunchedEffect(Unit) {
        // 先检查是否已有权限（可能用户之前已经授权过）
        if (permissionState.hasAnyLocationPermission()) {
            // 如果有权限，直接请求定位
            viewModel.onEvent(WeatherEvent.RequestLocation)
        } else {
            // 没有权限，弹出系统权限对话框
            // launch() 方法会触发系统的权限申请弹窗
            permissionLauncher.launch(locationPermissions)
        }
    }

    // ================== 步骤5：获取当前上下文 ==================
    // LocalContext.current - Compose 提供的环境变量，获取当前 Activity 的 Context
    // 用于跳转到系统设置页面
    val context = LocalContext.current

    // ================== 步骤6：根据状态决定显示什么界面 ==================
    // when 表达式按优先级依次判断，匹配到一个就显示对应的 UI
    // 这种写法叫做 "状态驱动的 UI"，是 Compose 的核心思想
    when {
        // 【情况1】权限未授予 - 显示权限说明界面
        // !permissionState.hasAnyLocationPermission() - 检查是否有任一位置权限
        !permissionState.hasAnyLocationPermission() -> {
            PermissionRationaleView(
                // 是否应该显示权限说明（用户拒绝过但没有勾选"不再询问"）
                shouldShowRationale = permissionState.shouldShowPermissionRationale,
                // 点击"授予权限"按钮时的回调
                onRequestPermission = { permissionLauncher.launch(locationPermissions) },
                // 点击"去设置中手动开启"时的回调
                onOpenSettings = {
                    openAppSettings(context)  // 跳转到应用设置页面
                }
            )
        }

        // 【情况2】加载中状态 - 显示转圈动画
        // 只要定位或天气数据任一在加载，就显示 Loading
        locationState.isLoading || weatherState.isLoading
            -> {
            LoadingScreen()
        }

        // 【情况3】定位出错 - 显示错误界面
        // locationState.error != null - 定位过程中出错
        locationState.error != null -> {
            ErrorScreen(
                message = locationState.error!!,  // 错误信息
                onRetry = { viewModel.onEvent(WeatherEvent.RequestLocation) }  // 点击重试
            )
        }

        // 【情况4】天气数据出错 - 显示错误界面
        weatherState.error != null -> {
            ErrorScreen(
                message = weatherState.error!!,
                onRetry = {
                    viewModel.onEvent(WeatherEvent.RefreshData)  // 点击刷新
                }
            )
        }

        // 【情况5】正常情况 - 显示天气内容
        // currentWeather != null 说明有天气数据
        weatherState.currentWeather != null -> {
            WeatherContent(
                weatherState = weatherState,
                onRefresh = {
                    viewModel.onEvent(WeatherEvent.RefreshData)  // 下拉刷新
                }
            )
        }

        // 【情况6】兜底情况 - 未知状态
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

        // 每日预报
        if (weatherState.forecast.isNotEmpty()) {
            DailyForecastSection(
                forecasts = weatherState.forecast
            )
        }
    }
}

@Composable
fun DailyForecastSection(forecasts: List<Forecast>) {
    val dailyForecasts = forecasts.groupBy {
        Instant.ofEpochSecond(it.dt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }.entries.take(15) //但这个免费api只有未来5天

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "每日预报",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dailyForecasts.forEach { (date, dayForecasts) ->
                val maxTemp = dayForecasts.maxOf { it.main.temp_max }.toInt()
                val minTemp = dayForecasts.minOf { it.main.temp_min }.toInt()

                val weather = dayForecasts.first().weather.first()

                DailyForecastItem(
                    dt = dayForecasts.first().dt,
                    weather = weather,
                    maxTemp = maxTemp,
                    minTemp = minTemp
                )
            }
        }
    }
}


@Composable
private fun DailyForecastItem(
    dt: Long,
    weather: Weather,
    maxTemp: Int,
    minTemp: Int
) {
    val weatherDesc = remember(weather) {
        when (weather.main.lowercase(Locale.CHINA)) {
            "clear" -> "晴"
            "clouds" -> "多云"
            "rain" -> "雨"
            else -> weather.description
        }
    }

    val dateLabel = remember(dt) {
        DateFormatter.getSmartDateLabel(dt)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        AsyncImage(
            model = "https://openweathermap.org/img/wn/${weather.icon}.png",
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )

        // 天气描述
        Text(
            text = weatherDesc,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "$maxTemp°",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "$minTemp°",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

