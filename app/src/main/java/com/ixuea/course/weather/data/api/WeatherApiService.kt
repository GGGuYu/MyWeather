package com.ixuea.course.weather.data.api

import com.ixuea.course.weather.config.Config
import com.ixuea.course.weather.data.model.AirQualityResponse
import com.ixuea.course.weather.data.model.WeatherResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * WeatherApiService - 天气 API 服务接口
 *
 * 【这是什么？】
 * 定义了与 OpenWeatherMap API 通信的所有网络请求方法
 * 使用 Retrofit 库自动生成 HTTP 请求代码
 *
 * 【为什么用 interface？】
 * Retrofit 使用动态代理技术，在运行时自动生成实现类
 * 你只需要定义接口和注解，Retrofit 帮你处理：
 * - HTTP 请求构建
 * - URL 拼接
 * - JSON 解析
 * - 线程切换
 *
 * 【类比】
 * 就像餐厅的点餐单（接口），客人只需要勾选想要的菜品（调用方法）
 * 后厨（Retrofit）会自动做菜（发送 HTTP 请求）并端上来（返回数据）
 */
interface WeatherApiService {

    /**
     * 获取当前天气数据
     *
     * 【注解说明】
     * @GET("data/2.5/weather") - HTTP GET 请求，路径是 data/2.5/weather
     *
     * 【参数说明】
     * @Query("lat") lat: Double - URL 查询参数 ?lat=39.9
     * @Query("lon") lon: Double - URL 查询参数 ?lon=116.4
     * @Query("appid") apiKey: String - API 密钥（OpenWeatherMap 需要）
     * @Query("lang") lang: String - 语言（默认中文 zh_cn）
     * @Query("units") units: String - 单位（默认 metric = 摄氏度）
     *
     * 【suspend 关键字】
     * 表示这是挂起函数，必须在协程中调用
     * 网络请求是耗时操作，suspend 让它不会阻塞主线程
     *
     * 【完整 URL 示例】
     * https://api.openweathermap.org/data/2.5/weather?lat=39.9&lon=116.4&appid=xxx&lang=zh_cn&units=metric
     */
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("lang") lang: String = "zh_cn",
        @Query("units") units: String = "metric"
    ): WeatherResponse

    /**
     * 获取空气质量数据
     *
     * 【API 说明】
     * 这个接口返回指定坐标的空气质量指数（AQI）和各污染物浓度
     * 包括：PM2.5、PM10、CO、NO、NO2、O3、SO2 等
     */
    @GET("data/2.5/air_pollution")
    suspend fun getAirQuality(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String = Config.API_KEY  // 使用默认参数，调用时可不传
    ): AirQualityResponse

    /**
     * 获取未来天气预报
     *
     * 【API 说明】
     * 返回未来 5 天、每 3 小时的数据（共 40 个时间点）
     * 包含：温度、湿度、天气状况、风速等
     *
     * 【数据量】
     * 5 天 × 8 次/天（每3小时一次）= 40 条数据
     */
    @GET("data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String = Config.API_KEY,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    /**
     * 伴生对象 - 创建 API Service 实例的工厂方法
     *
     * 【设计模式：工厂模式】
     * 提供一个静态方法 create() 来创建实例
     * 隐藏创建细节，调用者不需要知道 Retrofit 配置
     *
     * 【为什么放这里？】
     * 接口本身不负责创建，但创建这个接口的代码放在这里最自然
     * 类似于：点餐单旁边附上"如何获得这份菜单"的说明
     */
    companion object {
        // 基础 URL，所有接口都以这个开头
        const val BASE_URL = "https://api.openweathermap.org/"

        /**
         * 创建 WeatherApiService 实例
         *
         * 【创建过程】
         * 1. 配置 OkHttpClient（HTTP 客户端）
         * 2. 配置 Retrofit（REST 客户端）
         * 3. 使用 Retrofit 创建接口实现
         *
         * 【OkHttpClient 配置详解】
         */
        fun create(): WeatherApiService {
            // ========== 第一步：配置 OkHttpClient ==========
            // OkHttp 是底层的 HTTP 客户端，负责真正发送网络请求
            val client = OkHttpClient.Builder()
                // 添加日志拦截器 - 打印请求和响应信息（调试用）
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    // BASIC 级别会打印：请求方法、URL、状态码、响应大小
                })
                // 连接超时：10 秒
                // 含义：和服务器建立连接的最长时间
                .connectTimeout(10, TimeUnit.SECONDS)
                // 读取超时：15 秒
                // 含义：连接成功后，等待服务器响应数据的最长时间
                .readTimeout(15, TimeUnit.SECONDS)
                // 写入超时：15 秒
                // 含义：发送请求体（POST 数据）的最长时间
                .writeTimeout(15, TimeUnit.SECONDS)
                // 整个调用超时：30 秒
                // 含义：从开始请求到获得完整响应的总时间（包括重定向）
                .callTimeout(30, TimeUnit.SECONDS)
                .build()

            // ========== 第二步：配置 Retrofit ==========
            // Retrofit 是基于 OkHttp 的高级封装，专为 REST API 设计
            return Retrofit.Builder()
                .baseUrl(BASE_URL)                          // 基础 URL
                .client(client)                             // 使用上面配置的 OkHttpClient
                .addConverterFactory(GsonConverterFactory.create())  // JSON 解析器
                .build()
                // ========== 第三步：创建接口实现 ==========
                // create() 使用动态代理生成 WeatherApiService 的实现类
                // 你不用写实现代码，Retrofit 自动帮你生成 HTTP 请求逻辑
                .create(WeatherApiService::class.java)
        }
    }
}

/**
 * 【Retrofit 工作流程总结】
 *
 * 1. 定义接口 + 注解
 *    @GET("data/2.5/weather")
 *    suspend fun getCurrentWeather(...): WeatherResponse
 *
 * 2. Retrofit 生成实现
 *    - 解析注解，构建 HTTP 请求
 *    - 使用 OkHttp 发送请求
 *    - 接收 JSON 响应
 *    - 用 Gson 把 JSON 转换成 WeatherResponse 对象
 *    - 返回对象
 *
 * 3. 调用者（Repository）使用
 *    val weather = apiService.getCurrentWeather(39.9, 116.4, "xxx")
 */
