package com.ixuea.course.weather.data.repository

import com.ixuea.course.weather.config.Config
import com.ixuea.course.weather.data.api.WeatherApiService
import com.ixuea.course.weather.data.model.AirQualityResponse

/**
 * WeatherRepository - 天气数据仓库
 *
 * 【这是什么？】
 * Repository 是数据层和 ViewModel 之间的桥梁
 * 负责协调各种数据源（网络 API、本地数据库、缓存等）
 *
 * 【职责】
 * 1. 为 ViewModel 提供统一的数据获取接口
 * 2. 决定数据从哪来（网络/本地/缓存）
 * 3. 处理数据转换（如：合并多个接口的数据）
 * 4. 统一错误处理
 *
 * 【为什么需要 Repository？】
 * 如果没有 Repository，ViewModel 直接调用 ApiService：
 *   ViewModel → ApiService → HTTP 请求
 *
 * 有了 Repository，层次更清晰：
 *   ViewModel → Repository → ApiService → HTTP 请求
 *                       ↓
 *                  可以扩展：
 *                  - 添加本地数据库缓存
 *                  - 添加内存缓存
 *                  - 合并多个数据源
 *
 * 【类比】
 * Repository 像超市的仓库管理员：
 * - 前台（ViewModel）要货时，仓库管理员决定从哪取货
 * - 可能从货架（内存缓存）、库房（本地数据库）、或者联系供应商（网络 API）
 * - 前台不需要知道货从哪来，只管要就行
 *
 * 【当前项目简化】
 * 这个 Repository 比较简单，只是透传 ApiService 的方法
 * 因为目前只有网络数据源，没有本地缓存
 * 实际项目中通常会加：Room 数据库、SharedPreferences 等
 */
class WeatherRepository(private val apiService: WeatherApiService) {

    /**
     * 获取当前天气
     *
     * 【参数】
     * @param lat 纬度
     * @param lon 经度
     *
     * 【返回值】
     * WeatherResponse - 包含温度、湿度、天气状况等
     *
     * 【suspend 关键字】
     * 网络请求是耗时操作，必须在协程中调用
     * 这样不会阻塞主线程（UI 不会卡住）
     *
     * 【为什么透传？】
     * 当前只是简单调用 apiService，没有额外逻辑
     * 未来可以扩展：
     * - 先查本地缓存，有缓存且未过期就返回缓存
     * - 缓存过期或没有，再请求网络
     * - 网络请求成功后，保存到本地缓存
     */
    suspend fun getCurrentWeather(lat: Double, lon: Double) =
        apiService.getCurrentWeather(lat, lon, Config.API_KEY)

    /**
     * 获取空气质量
     *
     * 【显式指定返回类型】
     * 这里写了 : AirQualityResponse，和上面省略返回类型效果一样
     * 两种写法 Kotlin 都支持
     */
    suspend fun getAirQuality(lat: Double, lon: Double): AirQualityResponse {
        return apiService.getAirQuality(lat, lon)
    }

    /**
     * 获取天气预报
     *
     * 【返回数据结构】
     * WeatherResponse.list 包含 40 条数据（5天×8次）
     * 每条包含：时间、温度、天气图标等
     */
    suspend fun getWeatherForecast(lat: Double, lon: Double) =
        apiService.getWeatherForecast(lat, lon)
}

/**
 * 【Repository 模式的优势】
 *
 * 1. 单一数据源（Single Source of Truth）
 *    ViewModel 只和 Repository 交互，不直接操作网络/数据库
 *    Repository 决定数据从哪里来，保证数据一致性
 *
 * 2. 易于测试
 *    测试 ViewModel 时，可以用 MockRepository 替换真实 Repository
 *    不需要真的发送网络请求
 *
 * 3. 易于扩展
 *    添加本地缓存时，只需要改 Repository，ViewModel 无感知
 *
 * 4. 关注点分离
 *    - ViewModel：管理 UI 状态
 *    - Repository：管理数据获取
 *    - ApiService：管理网络请求细节
 *
 * 【典型 Repository 扩展示例】
 *
 * class WeatherRepository(
 *     private val apiService: WeatherApiService,
 *     private val weatherDao: WeatherDao,  // Room 数据库
 *     private val prefs: SharedPreferences // 缓存时间
 * ) {
 *     suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse {
 *         // 1. 检查缓存是否有效
 *         val cached = weatherDao.getWeather(lat, lon)
 *         if (cached != null && !isCacheExpired()) {
 *             return cached  // 返回缓存
 *         }
 *
 *         // 2. 缓存无效，请求网络
 *         val weather = apiService.getCurrentWeather(lat, lon, Config.API_KEY)
 *
 *         // 3. 保存到数据库
 *         weatherDao.insertWeather(weather)
 *         saveCacheTime()
 *
 *         return weather
 *     }
 * }
 */
