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

interface LocationClient {
    fun getLocation(): Flow<Resource<LocationData>>

    companion object {
        const val TAG = "LocationClient"
    }
}

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationClient {
    override fun getLocation(): Flow<Resource<LocationData>> = callbackFlow {
        // 显式检查权限（使用checkPermission）
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            trySend(Resource.Error("Location permissions not granted"))
            close()
            return@callbackFlow
        }

        // 构建位置请求（根据获得的权限选择精度）
        val priority = if (fineLocationGranted) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val request = LocationRequest.Builder(priority, 10_000L).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    trySend(Resource.Success(LocationData(location.latitude, location.longitude)))
                    close()
                }
            }
        }

        trySend(Resource.Loading())

        // 再次验证权限（防止竞态条件）
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

        client.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
            .addOnFailureListener { e ->
                trySend(Resource.Error("Location request failed: ${e.localizedMessage}"))
                close()
            }

        awaitClose {
            client.removeLocationUpdates(locationCallback)
        }
    }

}

sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
}