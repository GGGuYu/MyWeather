package com.ixuea.course.ui


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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val permissionState by viewModel.permissionState.collectAsState()
    val locationState by viewModel.locationState.collectAsState()

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
        locationState.isLoading
//                || weatherState.isLoading
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

        else -> {
            EmptyWeather()
        }
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

