package com.ixuea.course.weather.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.ixuea.course.weather.ui.LocationData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * LocationClient - 定位客户端接口
 *
 * 【为什么用接口？】
 * 1. 解耦：ViewModel 只依赖接口，不依赖具体实现
 * 2. 可测试：测试时可以用 Mock 实现替代真实定位
 * 3. 可替换：未来可以换成其他定位 SDK（如高德、百度）
 *
 * 【类比】
 * 就像电源插座接口，不管是哪个品牌的插头，只要符合接口规格都能用
 */
interface LocationClient {
    /**
     * 获取设备位置
     *
     * 【返回值】Flow<Resource<LocationData>>
     * - Flow: 数据流，可能发出多个结果（Loading → Success/Error）
     * - Resource: 封装结果状态（成功/失败/加载中）
     * - LocationData: 经纬度数据
     */
    fun getLocation(): Flow<Resource<LocationData>>

    companion object {
        const val TAG = "LocationClient"
    }
}

/**
 * DefaultLocationClient - 默认定位客户端实现
 *
 * 【职责】
 * 封装 Google Play Services 的定位功能，对外提供简洁的 Flow 接口
 *
 * 【依赖】
 * - Context: 用于检查权限
 * - FusedLocationProviderClient: Google 的定位服务客户端
 *
 * 【FusedLocationProviderClient 是什么？】
 * Google 提供的智能定位 API，会自动选择最优定位方式：
 * - GPS 定位：精度高（~10米），耗电高
 * - 网络定位（WiFi/基站）：精度低（~100米），省电
 * - 融合定位：综合多种方式，平衡精度和耗电
 */
class DefaultLocationClient(
    private val context: Context,                           // Android 上下文，用于检查权限
    private val client: FusedLocationProviderClient,        // Google 定位服务客户端
) : LocationClient {

    /**
     * getLocation - 获取设备位置
     *
     * 【核心概念：callbackFlow】
     * 把基于回调的 API 转换成基于 Flow 的 API
     *
     * 【为什么需要 callbackFlow？】
     * Google 定位 API 是回调模式：
     *   client.requestLocationUpdates(request, callback, looper)
     *   ↓
     *   callback.onLocationResult(result)  // 异步回调
     *
     * 但我们想用 Flow（更优雅、支持协程），所以需要 callbackFlow 做转换：
     *   回调 → Flow
     *
     * 【callbackFlow 工作原理】
     * 1. 创建一个"热流"（可以主动发数据）
     * 2. 用 trySend() 发送数据到流中
     * 3. 用 awaitClose() 等待流关闭，执行清理
     */
    override fun getLocation(): Flow<Resource<LocationData>> = callbackFlow {
        // ============================================================
        // 第一步：检查权限
        // ============================================================
        // 【为什么还要检查权限？ViewModel 不是已经检查过了吗？】
        // 1. 防御性编程：不信任外部调用
        // 2. 安全门：防止有人绕过 ViewModel 直接调用这个方法
        // 3. Google SDK 要求：调用定位 API 前必须检查权限，否则崩溃

        // 检查精确定位权限
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 检查粗略定位权限
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // 两个权限都没有，直接返回错误
        if (!fineLocationGranted && !coarseLocationGranted) {
            // trySend: 向 Flow 发送数据（非挂起函数，可以在任何地方调用）
            trySend(Resource.Error("Location permissions not granted"))
            // close: 关闭 Flow，不再发送数据
            close()
            // return@callbackFlow: 提前退出 callbackFlow 代码块
            return@callbackFlow
        }

        // ============================================================
        // 第二步：构建定位请求
        // ============================================================
        // 【两种定位精度的区别】
        // - PRIORITY_HIGH_ACCURACY: 高精度（GPS + 网络），精度 ~10米
        // - PRIORITY_BALANCED_POWER_ACCURACY: 平衡（主要网络），精度 ~100米
        //
        // 【为什么根据权限选择精度？】
        // 用户可能只给了粗略定位权限，没给精确定位权限
        // 这时用高精度请求会失败，需要降级到平衡精度

        val priority = if (fineLocationGranted) {
            Priority.PRIORITY_HIGH_ACCURACY           // 高精度（用户给了精确定位权限）
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY // 平衡精度（只有粗略定位权限）
        }

        // 构建定位请求
        // LocationRequest.Builder(priority, intervalMillis)
        // - priority: 精度等级
        // - intervalMillis: 定位间隔（10_000L = 10秒）
        val request = LocationRequest.Builder(priority, 10_000L).build()

        // ============================================================
        // 第三步：创建定位回调
        // ============================================================
        // 【什么是回调？】
        // 你给系统一个"电话号码"（回调对象），等系统定位完成后"打给你"（调用回调方法）
        //
        // 【LocationCallback 的工作方式】
        // 系统定位成功后 → 调用 onLocationResult() → 我们在这里处理结果

        val locationCallback = object : LocationCallback() {
            /**
             * onLocationResult - 定位结果回调
             *
             * 【什么时候被调用？】
             * 系统获取到位置后，会调用这个方法
             *
             * 【result.locations 是什么？】
             * 可能包含多个位置点（如果开启持续定位）
             * 我们只需要最新的一个，所以用 lastOrNull()
             */
            override fun onLocationResult(result: LocationResult) {
                // 获取最后一个位置点（最新的）
                result.locations.lastOrNull()?.let { location ->
                    // 发送成功结果到 Flow
                    // location.latitude: 纬度（-90 到 90）
                    // location.longitude: 经度（-180 到 180）
                    trySend(Resource.Success(LocationData(location.latitude, location.longitude)))
                    // 只要一次定位，拿到结果就关闭 Flow
                    close()
                }
            }
        }

        // ============================================================
        // 第四步：发送"加载中"状态
        // ============================================================
        // 告诉订阅者：定位开始了，请稍等
        trySend(Resource.Loading())

        // ============================================================
        // 第五步：再次检查权限（防止竞态条件）
        // ============================================================
        // 【什么是竞态条件？】
        // 假设这样的场景：
        // 1. 用户打开 App，开始定位
        // 2. 定位过程中，用户去系统设置，把定位权限关掉
        // 3. 定位完成，回调触发，但此时已经没有权限了
        //
        // 这种"权限在定位过程中被撤销"的情况就是竞态条件
        // 虽然概率很小，但为了安全，再检查一次

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            trySend(Resource.Error("Permissions revoked during request"))
            close()
            return@callbackFlow
        }

        // ============================================================
        // 第六步：请求定位
        // ============================================================
        // 【requestLocationUpdates 参数说明】
        // - request: 定位请求配置（精度、间隔等）
        // - locationCallback: 定位结果回调
        // - Looper.getMainLooper(): 在主线程处理回调（UI 更新必须在主线程）
        //
        // 【返回值】
        // 返回一个 Task 对象，可以添加成功/失败监听

        client.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
            .addOnFailureListener { e ->
                // 定位请求失败（比如用户关闭了 GPS）
                trySend(Resource.Error("Location request failed: ${e.localizedMessage}"))
                close()
            }

        // ============================================================
        // 第七步：等待关闭
        // ============================================================
        // 【awaitClose 的作用】
        // 1. 挂起当前协程，等待 Flow 被关闭
        // 2. 当 Flow 被取消时（比如 ViewModel 销毁），执行清理代码
        //
        // 【为什么需要清理？】
        // 如果不移除回调，定位服务会一直运行，浪费电量
        // 这就像打电话后要挂断，否则电话一直占线

        awaitClose {
            // 清理：移除定位回调，停止定位
            client.removeLocationUpdates(locationCallback)
        }
    }

}

/**
 * Resource - 资源结果封装类
 *
 * 【为什么需要这个类？】
 * 网络请求、定位等异步操作有三种状态：
 * 1. Loading - 正在进行
 * 2. Success - 成功，携带数据
 * 3. Error - 失败，携带错误信息
 *
 * 用 sealed class 可以优雅地表示这三种状态
 *
 * 【为什么用 sealed class？】
 * 1. 限定子类数量（只有 Success、Error、Loading 三种）
 * 2. 配合 when 使用，编译器强制处理所有情况
 * 3. 类型安全，不需要强制类型转换
 *
 * 【泛型 T 的作用】
 * 可以包装任何类型的数据
 * - Resource<LocationData> 包装定位数据
 * - Resource<WeatherResponse> 包装天气数据
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    /**
     * Success - 成功状态
     * @param data 成功返回的数据
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Error - 错误状态
     * @param message 错误信息
     * @param data 可选的额外数据（比如部分成功的数据）
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Loading - 加载中状态
     * 不需要携带数据
     */
    class Loading<T> : Resource<T>()
}
