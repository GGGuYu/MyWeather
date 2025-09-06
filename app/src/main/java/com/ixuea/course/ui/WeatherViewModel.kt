package com.ixuea.course.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 添加ViewModel
 */
class WeatherViewModel : ViewModel() {
    /**
     * 权限状态
     */
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState


    /**
     * 事件入口
     */
    fun onEvent(permissionResult: WeatherEvent) {

    }
}

/**
 * 事件密封类
 */
sealed class WeatherEvent {
    data class PermissionResult(val grantedPermissions: Map<String, Boolean>) : WeatherEvent()
    object RequestLocation : WeatherEvent()
}

/**
 * 权限状态
 */
data class PermissionState(
    val fineLocationGranted: Boolean = false,
    val coarseLocationGranted: Boolean = false,
    val shouldShowPermissionRationale: Boolean = false
) {
    fun hasFineLocation() = fineLocationGranted
    fun hasCoarseLocation() = coarseLocationGranted
    fun hasAnyLocationPermission() = fineLocationGranted || coarseLocationGranted
}

class WeatherViewModelFactory() : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}