package com.booktube.bluetooththingsfinder.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SignalCellularAlt1Bar
import androidx.compose.material.icons.outlined.SignalCellularAlt2Bar
import androidx.compose.material.icons.outlined.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.outlined.SignalCellularNodata
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.booktube.bluetooththingsfinder.model.DeviceType

/**
 * Displays an icon representing a Bluetooth device type.
 *
 * @param deviceType The type of the device
 * @param modifier Modifier to be applied to the icon
 * @param size The size of the icon (defaults to 24dp)
 * @param tint The tint color of the icon (defaults to primary color)
 */
@Composable
fun DeviceIcon(
    deviceType: DeviceType,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    val icon: ImageVector = when (deviceType) {
        DeviceType.BLE -> Icons.Default.BluetoothAudio
        DeviceType.CLASSIC -> Icons.Default.Devices
        DeviceType.DUAL -> Icons.Default.Bluetooth
    }
    
    Icon(
        imageVector = icon,
        contentDescription = null, // Decorative element
        modifier = modifier.size(size),
        tint = tint
    )
}

/**
 * Displays a signal strength indicator based on RSSI value.
 *
 * @param rssi The Received Signal Strength Indicator in dBm
 * @param modifier Modifier to be applied to the icon
 * @param size The size of the icon (defaults to 16dp)
 */
@Composable
fun SignalStrengthIndicator(
    rssi: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp
) {
    val icon: ImageVector = when {
        rssi == null -> Icons.Outlined.SignalCellularNodata
        rssi >= -50 -> Icons.Outlined.SignalCellularAlt // Excellent (4 bars)
        rssi >= -70 -> Icons.Outlined.SignalCellularAlt2Bar // Good (3 bars)
        rssi >= -80 -> Icons.Outlined.SignalCellularAlt1Bar // Fair (2 bars)
        rssi >= -90 -> Icons.Outlined.SignalCellularAlt1Bar // Poor (1 bar)
        else -> Icons.Outlined.SignalCellularConnectedNoInternet0Bar // No signal
    }
    
    val tint = when {
        rssi == null -> MaterialTheme.colorScheme.onSurfaceVariant
        rssi >= -70 -> MaterialTheme.colorScheme.primary // Green for good signal
        rssi >= -80 -> MaterialTheme.colorScheme.secondary // Yellow for fair signal
        else -> MaterialTheme.colorScheme.error // Red for poor signal
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Signal strength: $rssi dBm",
        modifier = modifier.size(size),
        tint = tint
    )
}
