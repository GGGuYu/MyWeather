package com.ixuea.course.ui

import android.Manifest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ixuea.course.ui.data.location.LocationClient
import com.ixuea.course.ui.data.location.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * 添加ViewModel
 */
class WeatherViewModel(private val locationClient: LocationClient) : ViewModel() {
    /**
     * 权限状态
     */
    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    /**
     * 事件入口
     */
    fun onEvent(event: WeatherEvent) {
//        Log.d(TAG, "onEvent: ${permissionResult}")
        
        when (event) {
            is WeatherEvent.PermissionResult -> handlePermissionResult(event.grantedPermissions)
            is WeatherEvent.RequestLocation -> requestLocationWithPermissionCheck()
            is WeatherEvent.LocationUpdate -> onLocationChanged(event.locationData)
        }
    }

    private fun onLocationChanged(location: LocationData) {
        _locationState.update {
            it.copy(
                location = location,
                isLoading = false,
                error = null
            )
        }

        fetchWeatherData(location)
    }

    private fun fetchWeatherData(location: LocationData) {
        Log.d(TAG, "fetchWeatherData: ${location}")
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
        _locationState.update { it.copy(isLoading = true, error = null) }

        locationClient.getLocation()
            .onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { location ->
                            onEvent(WeatherEvent.LocationUpdate(location))
                        }

                    }

                    is Resource.Error -> {
                        handleError(
                            message = result.message ?: "Location error",
                            errorType = ErrorType.LOCATION_FAILURE
                        )
                    }

                    is Resource.Loading -> {
                        _locationState.update { it.copy(isLoading = true) }
                    }
                }
            }
            .launchIn(viewModelScope)
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
    data class LocationUpdate(val locationData: LocationData) : WeatherEvent()
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

data class LocationState(
    val location: LocationData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

class WeatherViewModelFactory(private val locationClient: LocationClient) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(locationClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}