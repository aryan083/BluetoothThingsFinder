package com.booktube.bluetooththingsfinder.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Check if the app has been granted a specific permission.
 * 
 * @param permission The permission to check, e.g., [Manifest.permission.ACCESS_FINE_LOCATION]
 * @return `true` if the permission is granted, `false` otherwise
 */
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Check if the app has all the required Bluetooth and location permissions.
 * 
 * @return `true` if all required permissions are granted, `false` otherwise
 */
fun Context.hasRequiredBluetoothPermissions(): Boolean {
    return hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
           hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
           hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
}

/**
 * A composable that requests the required Bluetooth and location permissions.
 * 
 * @param onPermissionResult Callback with the result of the permission request
 * @param onPermissionsGranted Callback invoked when all permissions are granted
 * @param onPermissionsDenied Callback invoked when any permission is denied
 */
@Composable
fun RequestBluetoothPermissions(
    onPermissionResult: (Boolean) -> Unit = {},
    onPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissions = remember {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        onPermissionResult(allGranted)
        
        if (allGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }
    
    // Check permissions when the composable is first launched
    SideEffect {
        val hasAllPermissions = permissions.all { permission ->
            context.hasPermission(permission)
        }
        
        if (!hasAllPermissions) {
            permissionLauncher.launch(permissions)
        } else {
            onPermissionsGranted()
        }
    }
}

/**
 * Check if the device supports Bluetooth Low Energy (BLE).
 * 
 * @return `true` if BLE is supported, `false` otherwise
 */
fun Context.isBleSupported(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
}

/**
 * Check if the device supports Bluetooth Classic.
 * 
 * @return `true` if Bluetooth Classic is supported, `false` otherwise
 */
fun Context.isBluetoothClassicSupported(): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
}

annotation class PermissionUtils
