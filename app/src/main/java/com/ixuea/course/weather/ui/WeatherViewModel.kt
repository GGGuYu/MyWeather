package com.ixuea.course.weather.ui

import android.Manifest
import android.util.Log
// ViewModel 基类 - 管理UI相关数据， survives 配置更改（如屏幕旋转）
import androidx.lifecycle.ViewModel
// ViewModelProvider.Factory - 用于创建带参数的 ViewModel
import androidx.lifecycle.ViewModelProvider
// viewModelScope - ViewModel 专用的协程作用域，自动取消协程
import androidx.lifecycle.viewModelScope
import com.ixuea.course.weather.data.location.LocationClient
import com.ixuea.course.weather.data.location.Resource
import com.ixuea.course.weather.data.model.AirQualityResponse
import com.ixuea.course.weather.data.model.Forecast
import com.ixuea.course.weather.data.model.WeatherResponse
import com.ixuea.course.weather.data.repository.WeatherRepository
import com.ixuea.course.weather.utils.AQICalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ================== ViewModel 核心类 ==================

/**
 * WeatherViewModel - 天气应用的 ViewModel
 * 
 * 【职责】
 * 1. 管理 UI 状态（权限、定位、天气数据）
 * 2. 处理业务逻辑（权限检查、定位、网络请求）
 * 3. 协调 Repository 和 LocationClient
 * 
 * 【为什么需要 ViewModel？】
 * - UI 不应该直接处理业务逻辑
 * - ViewModel 在屏幕旋转等配置更改时不会销毁，数据保持
 * - 方便单元测试（逻辑与 UI 分离）
 * 
 * 【依赖注入】
 * 通过构造函数传入 repository 和 locationClient
 * 这样可以方便测试时替换成 Mock 对象
 */
class WeatherViewModel(
    private val repository: WeatherRepository,      // 网络请求仓库
    private val locationClient: LocationClient      // 定位客户端
) : ViewModel() {

    // ================== 状态定义 ==================
    // 【设计模式】单一数据源（Single Source of Truth）
    // 所有 UI 状态都由 ViewModel 持有和管理

    /**
     * 权限状态
     * 
     * 【为什么用 MutableStateFlow？】
     * - 线程安全
     * - 可以被多个观察者订阅
     * - 自动通知观察者数据变化
     * 
     * 【为什么用两个变量（私有可变 + 公开只读）？】
     * - _permissionState: 只在 ViewModel 内部可修改（封装）
     * - permissionState: 对外暴露只读，防止外部意外修改
     */
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState

    /**
     * 定位状态
     * 
     * 【asStateFlow() 的作用】
     * 将 MutableStateFlow 转为只读 StateFlow
     * 比 = _locationState 更安全（虽然本项目 permissionState 没用也行）
     */
    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    /**
     * 天气数据状态
     */
    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    // ================== 事件入口 ==================

    /**
     * onEvent - 统一的事件处理入口
     * 
     * 【为什么用这个设计？】
     * 1. UI 只需要调用这一个方法，传入不同事件
     * 2. 所有事件处理逻辑集中在一起，易于维护
     * 3. 配合 sealed class，编译器保证处理所有事件类型
     * 
     * 【类比】
     * 就像一个公司的前台，所有来访（事件）都先到前台，
     * 然后前台分配到对应的部门（处理方法）处理
     */
    fun onEvent(event: WeatherEvent) {
        when (event) {
            // 用户回应了权限请求 → 处理权限结果
            is WeatherEvent.PermissionResult -> handlePermissionResult(event.grantedPermissions)
            // 请求获取位置 → 先检查权限再定位
            is WeatherEvent.RequestLocation -> requestLocationWithPermissionCheck()
            // 位置更新了 → 保存位置并请求天气
            is WeatherEvent.LocationUpdate -> onLocationChanged(event.locationData)
            // 天气数据加载完成 → 更新 UI 状态
            is WeatherEvent.WeatherDataLoaded -> updateWeatherData(
                event.weather,
                event.forecast,
                event.airQuality
            )
            // 用户下拉刷新 → 重新加载所有数据
            is WeatherEvent.RefreshData -> refreshAllData()
            // 发生错误 → 统一错误处理
            is WeatherEvent.Error -> handleError(event.message, event.errorType)
        }
    }

    // ================== 具体业务方法 ==================

    /**
     * refreshAllData - 刷新所有数据
     * 
     * 【触发场景】
     * - 用户下拉刷新
     * - 网络错误后点击重试
     * 
     * 【为什么存在？】
     * 提供一个统一的刷新入口，重新走一遍定位+请求流程
     */
    private fun refreshAllData() {
        // 先设置为加载状态（UI 会显示转圈）
        _weatherState.update { it.copy(isLoading = true) }
        // 重新请求位置（权限检查 -> 定位 -> 请求天气）
        requestLocationWithPermissionCheck()
    }

    /**
     * updateWeatherData - 更新天气数据到状态
     * 
     * 【为什么存在？】
     * 网络请求成功后，需要把数据存到 weatherState
     * 同时计算空气质量指数（AQI）
     * 
     * 【为什么在这里计算 AQI？】
     * API 返回的是原始污染物数据，需要转换成用户友好的 AQI 等级
     */
    private fun updateWeatherData(
        weather: WeatherResponse,
        forecast: List<Forecast>,
        airQuality: AirQualityResponse
    ) {
        // 计算空气质量指数
        val aqiResult = AQICalculator.calculateAQIFromComponents(airQuality.list.first().components)

        // 更新状态（copy 创建新对象，触发 StateFlow 通知）
        _weatherState.update {
            it.copy(
                currentWeather = weather,   // 当前天气
                forecast = forecast,         // 未来预报
                aqiResult = aqiResult,       // 空气质量
                isLoading = false,           // 加载完成
                error = null,                // 清除错误
            )
        }
    }

    /**
     * onLocationChanged - 位置变化回调
     * 
     * 【为什么存在？】
     * 定位成功后需要：
     * 1. 保存位置信息（可能后续要用）
     * 2. 用这个位置去请求天气数据
     * 
     * 【设计思想】
     * 把"保存位置"和"请求天气"分开，职责单一
     */
    private fun onLocationChanged(location: LocationData) {
        // 更新定位状态
        _locationState.update {
            it.copy(
                location = location,   // 保存位置
                isLoading = false,     // 定位完成
                error = null           // 清除错误
            )
        }

        // 用这个位置去获取天气
        fetchWeatherData(location)
    }

    /**
     * fetchWeatherData - 获取天气数据（核心网络请求）
     * 
     * 【为什么存在？】
     * 这是真正发起网络请求的地方
     * 同时请求：当前天气 + 空气质量 + 未来预报
     * 
     * 【为什么用 viewModelScope.launch？】
     * - 协程在 ViewModel 销毁时自动取消
     * - 避免内存泄漏
     * - 不需要手动管理生命周期
     */
    private fun fetchWeatherData(location: LocationData) {
        Log.d(TAG, "fetchWeatherData: ${location}")

        // 设置加载状态
        _weatherState.update { it.copy(isLoading = true, error = null) }

        // 启动协程进行网络请求
        viewModelScope.launch {
            try {
                // 【并发请求】3个请求依次执行（也可以用 async 并行优化）
                
                // 1. 当前天气
                val currentWeather =
                    repository.getCurrentWeather(location.latitude, location.longitude)

                // 2. 空气质量
                val airQuality = repository.getAirQuality(
                    location.latitude,
                    location.longitude
                )

                // 3. 未来5天预报（每3小时一个数据点）
                val forecast = repository.getWeatherForecast(
                    location.latitude,
                    location.longitude
                )

                // 全部成功，发送"数据加载完成"事件
                onEvent(
                    WeatherEvent.WeatherDataLoaded(
                        weather = currentWeather,
                        forecast = forecast.list ?: emptyList(),
                        airQuality = airQuality,
                    )
                )
            } catch (e: Exception) {
                // 任意一个请求失败，统一处理错误
                handleError(
                    message = "Weather data load failed: ${e.localizedMessage}",
                    errorType = ErrorType.NETWORK_FAILURE
                )
            }
        }
    }

    /**
     * requestLocationWithPermissionCheck - 带权限检查的定位请求
     * 
     * 【为什么存在？】
     * 不能直接定位，必须先检查有没有权限
     * 这是一道安全门，防止没有权限时崩溃
     * 
     * 【设计思想】
     * 防御性编程 - 即使外部调用错了，也不会崩溃
     */
    private fun requestLocationWithPermissionCheck() {
        when {
            // 有精确定位权限 → 可以定位
            _permissionState.value.hasFineLocation() -> requestLocation()
            // 有粗略定位权限 → 也可以定位（精度低一点）
            _permissionState.value.hasCoarseLocation() -> requestLocation()
            // 都没有权限 → 报错（理论上不应该走到这里）
            else -> handleError(
                message = "Request location permission first",
                errorType = ErrorType.PERMISSION_REQUIRED
            )
        }
    }

    /**
     * handlePermissionResult - 处理权限请求结果
     * 
     * 【什么时候被调用？】
     * 用户在系统权限对话框点击"允许"或"拒绝"后
     * 
     * 【为什么存在？】
     * 1. 解析用户的选择（哪个权限给了，哪个没给）
     * 2. 更新权限状态
     * 3. 决定下一步（有权限就定位，没权限就报错）
     */
    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        // 从 Map 中提取结果，默认 false（如果 key 不存在）
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        // 更新权限状态
        _permissionState.update {
            it.copy(
                fineLocationGranted = fineGranted,
                coarseLocationGranted = coarseGranted
            )
        }

        // 根据权限结果决定下一步
        when {
            // 有任意一个权限就继续定位
            fineGranted || coarseGranted -> requestLocation()
            // 都没给，报错
            else -> handleError(
                message = "Location permission denied",
                errorType = ErrorType.PERMISSION_DENIED
            )
        }
    }

    /**
     * requestLocation - 真正执行定位
     * 
     * 【为什么存在？】
     * 调用 LocationClient 获取设备位置
     * 处理定位结果（成功/失败/加载中）
     * 
     * 【为什么用 Flow？】
     * 定位是一个持续的过程（可能多次回调）
     * Flow 可以优雅地处理数据流
     */
    private fun requestLocation() {
        // 设置定位加载状态
        _locationState.update { it.copy(isLoading = true, error = null) }

        // 订阅定位结果流
        locationClient.getLocation()
            .onEach { result ->
                // 每次收到结果时处理
                when (result) {
                    // 定位成功
                    is Resource.Success -> {
                        result.data?.let { location ->
                            // 发送位置更新事件
                            onEvent(WeatherEvent.LocationUpdate(location))
                        }
                    }

                    // 定位失败
                    is Resource.Error -> {
                        handleError(
                            message = result.message ?: "Location error",
                            errorType = ErrorType.LOCATION_FAILURE
                        )
                    }

                    // 定位中（继续等待）
                    is Resource.Loading -> {
                        _locationState.update { it.copy(isLoading = true) }
                    }
                }
            }
            .launchIn(viewModelScope)  // 在 ViewModel 作用域中启动
    }

    /**
     * handleError - 统一错误处理
     * 
     * 【为什么存在？】
     * 不同类型的错误需要更新不同的状态
     * 集中处理，避免重复代码
     * 
     * 【设计思想】
     * 错误类型（ErrorType）决定更新哪个状态
     */
    private fun handleError(message: String, errorType: ErrorType) {
        when (errorType) {
            // 权限被拒绝 → 更新权限状态，提示用户去设置开启
            ErrorType.PERMISSION_DENIED -> {
                _permissionState.update {
                    it.copy(shouldShowPermissionRationale = true)
                }
            }
            // 定位失败 → 更新定位状态，显示错误信息
            ErrorType.LOCATION_FAILURE -> {
                _locationState.update { it.copy(isLoading = false, error = message) }
            }
            // 网络失败 → 更新天气状态，显示错误信息
            ErrorType.NETWORK_FAILURE -> {
                _weatherState.update { it.copy(isLoading = false, error = message) }
            }

            // 其他错误 → 统一当作天气错误处理
            else -> {
                _weatherState.update { it.copy(isLoading = false, error = message) }
            }
        }
    }

    companion object {
        const val TAG = "WeatherViewModel"
    }
}

// ================== 错误类型枚举 ==================

/**
 * ErrorType - 错误类型枚举
 * 
 * 【为什么存在？】
 * 不同错误需要不同的处理方式
 * 用枚举比用字符串更安全（编译时检查）
 */
enum class ErrorType {
    PERMISSION_DENIED,    // 权限被拒绝
    PERMISSION_REQUIRED,  // 需要权限（还没有请求过）
    LOCATION_FAILURE,     // 定位失败
    NETWORK_FAILURE,      // 网络请求失败
}

// ================== 事件定义 ==================

/**
 * WeatherEvent - 事件密封类
 * 
 * 【为什么用 sealed class？】
 * 1. 限定事件类型，不能随意添加新事件
 * 2. 配合 when 使用，编译器强制处理所有情况
 * 3. 每个事件可以携带不同类型的数据
 * 
 * 【类比】
 * 就像公司规定的几种请假类型（病假、事假、年假...）
 * 每种类型需要提交不同的证明材料
 */
sealed class WeatherEvent {
    // 权限结果事件 - 携带权限授予情况的 Map
    data class PermissionResult(val grantedPermissions: Map<String, Boolean>) : WeatherEvent()
    
    // 请求定位事件 - 不需要携带数据
    object RequestLocation : WeatherEvent()
    
    // 位置更新事件 - 携带位置数据
    data class LocationUpdate(val locationData: LocationData) : WeatherEvent()
    
    // 天气数据加载完成事件 - 携带天气、预报、空气质量数据
    data class WeatherDataLoaded(
        val weather: WeatherResponse,
        val forecast: List<Forecast>,
        val airQuality: AirQualityResponse
    ) : WeatherEvent()
    
    // 刷新数据事件 - 不需要携带数据
    object RefreshData : WeatherEvent()
    
    // 错误事件 - 携带错误信息和类型
    data class Error(val message: String, val errorType: ErrorType) : WeatherEvent()
}

// ================== 状态数据类 ==================

/**
 * PermissionState - 权限状态
 * 
 * 【为什么用 data class？】
 * 1. 自动生成 copy() 方法，方便更新部分字段
 * 2. 自动生成 equals/hashCode/toString
 * 3. 不可变（val），线程安全
 */
data class PermissionState(
    val fineLocationGranted: Boolean = false,           // 精确定位权限
    val coarseLocationGranted: Boolean = false,         // 粗略定位权限
    val shouldShowPermissionRationale: Boolean = false, // 是否应该显示权限说明
) {
    // 便捷方法 - 检查是否有精确定位权限
    fun hasFineLocation() = fineLocationGranted
    // 便捷方法 - 检查是否有粗略定位权限
    fun hasCoarseLocation() = coarseLocationGranted
    // 便捷方法 - 检查是否有任意位置权限
    fun hasAnyLocationPermission() = fineLocationGranted || coarseLocationGranted
}

/**
 * LocationState - 定位状态
 * 
 * 【字段说明】
 * - location: 定位成功后的坐标（可能为空）
 * - isLoading: 是否正在定位
 * - error: 定位失败时的错误信息
 */
data class LocationState(
    val location: LocationData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * LocationData - 位置数据
 * 
 * 【为什么单独一个类？】
 * 封装经纬度，方便传递和复用
 */
data class LocationData(
    val latitude: Double,   // 纬度
    val longitude: Double   // 经度
)

/**
 * WeatherState - 天气数据状态
 * 
 * 【字段说明】
 * - currentWeather: 当前天气数据
 * - forecast: 未来天气预报列表
 * - aqiResult: 空气质量计算结果
 * - isLoading: 是否正在加载
 * - error: 加载失败时的错误信息
 */
data class WeatherState(
    val currentWeather: WeatherResponse? = null,
    val forecast: List<Forecast> = emptyList(),
    val aqiResult: AQICalculator.AQIResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ================== ViewModel 工厂类 ==================

/**
 * WeatherViewModelFactory - ViewModel 工厂
 * 
 * 【为什么需要 Factory？】
 * ViewModel 默认只能有无参构造函数
 * 但我们需要传入 repository 和 locationClient
 * 所以需要一个工厂来创建带参数的 ViewModel
 * 
 * 【使用方式】
 * 在 Activity 中：
 * val viewModel: WeatherViewModel by viewModels {
 *     WeatherViewModelFactory(repository, locationClient)
 * }
 */
class WeatherViewModelFactory(
    private val repository: WeatherRepository,
    private val locationClient: LocationClient
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // 检查请求的 ViewModel 类型是否匹配
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            // 创建并返回 ViewModel 实例
            return WeatherViewModel(repository, locationClient) as T
        }
        // 类型不匹配，抛出异常
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
