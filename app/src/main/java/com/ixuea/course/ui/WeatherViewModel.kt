package com.ixuea.course.ui

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

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
    fun onEvent(event: WeatherEvent) {
//        Log.d(TAG, "onEvent: ${permissionResult}")
        
        when (event) {
            is WeatherEvent.PermissionResult -> handlePermissionResult(event.grantedPermissions)
            is WeatherEvent.RequestLocation -> requestLocationWithPermissionCheck()
        }
    }

    private fun requestLocationWithPermissionCheck() {
        when {
            _permissionState.value.hasFineLocation() -> requestLocation()
            _permissionState.value.hasCoarseLocation() -> requestLocation()
            else -> handleError(
                message = "Request location permission first",
                errorType = ErrorType.PERMISSION_REQUIRED
            )
        }
    }

    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        _permissionState.update {
            it.copy(
                fineLocationGranted = fineGranted,
                coarseLocationGranted = coarseGranted
            )
        }

        when {
            fineGranted || coarseGranted -> requestLocation()
            else -> handleError(
                message = "Location permission denied",
                errorType = ErrorType.PERMISSION_DENIED
            )
        }
    }

    private fun requestLocation() {

    }

    private fun handleError(message: String, errorType: ErrorType) {
        when (errorType) {
            ErrorType.PERMISSION_DENIED -> {
                _permissionState.update {
                    it.copy(shouldShowPermissionRationale = true)
                }
            }

            else -> {

            }
//            ErrorType.LOCATION_FAILURE -> {
//                _locationState.update { it.copy(isLoading = false, error = message) }
//            }
//
//            ErrorType.NETWORK_FAILURE -> {
//                _weatherState.update { it.copy(isLoading = false, error = message) }
//            }
//
//            else -> {
//                _weatherState.update { it.copy(isLoading = false, error = message) }
//            }
        }
    }


    companion object {
        const val TAG = "WeatherViewModel"
    }
}

enum class ErrorType {
    PERMISSION_DENIED,
    PERMISSION_REQUIRED,
    LOCATION_FAILURE,
    NETWORK_FAILURE,
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
    val shouldShowPermissionRationale: Boolean = false,
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