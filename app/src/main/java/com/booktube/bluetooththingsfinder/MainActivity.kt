package com.booktube.bluetooththingsfinder

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.booktube.bluetooththingsfinder.ui.theme.BluetoothThingsFinderTheme
import androidx.compose.foundation.clickable
import android.util.Log

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothScanner: BluetoothScanner
    private lateinit var directionIndicator: DirectionIndicator
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startContinuousScan()
        } else {
            Toast.makeText(this, "Permissions required for Bluetooth scanning", Toast.LENGTH_LONG).show()
        }
    }
    
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startContinuousScan()
        } else {
            Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        directionIndicator = DirectionIndicator(this)
        bluetoothScanner = BluetoothScanner(this, bluetoothAdapter, directionIndicator)
        
        setContent {
            BluetoothThingsFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        bluetoothScanner = bluetoothScanner,
                        directionIndicator = directionIndicator,
                        onStartScan = { checkPermissionsAndStartScan() },
                        onStopScan = { bluetoothScanner.stopScan() },
                        onRefreshScan = { bluetoothScanner.refreshScan() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        directionIndicator.start()
        checkPermissionsAndStartScan()
    }
    
    override fun onPause() {
        super.onPause()
        directionIndicator.stop()
        bluetoothScanner.stopScan()
    }
    
    private fun checkPermissionsAndStartScan() {
        val permissions = mutableListOf<String>()
        
        // Add Bluetooth permissions based on API level
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        // Always add location permission
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isEmpty()) {
            startContinuousScan()
        } else {
            requestPermissionLauncher.launch(permissionsToRequest)
        }
    }
    
    private fun startContinuousScan() {
        if (!bluetoothAdapter.isEnabled) {
            // Check if we have BLUETOOTH_CONNECT permission before requesting to enable
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show()
                    return
                }
            }
            
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
            return
        }
        
        bluetoothScanner.startContinuousScan()
    }
}

@Composable
fun MainScreen(
    bluetoothScanner: BluetoothScanner,
    directionIndicator: DirectionIndicator,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onRefreshScan: () -> Unit
) {
    val devices by bluetoothScanner.devices.collectAsState()
    val isScanning by bluetoothScanner.isScanning.collectAsState()
    val currentDirection by directionIndicator.currentDirection.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp) // Safe area for status bar
    ) {
        // Header with proper safe area
        Text(
            text = "Bluetooth Things Finder",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        
        // Status indicator
//        if (isScanning) {
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 20.dp, vertical = 8.dp),
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer
//                )
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.BluetoothSearching,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.size(24.dp)
//                    )
//                    Spacer(modifier = Modifier.width(12.dp))
//                    Text(
//                        text = "Continuously scanning for devices...",
//                        color = MaterialTheme.colorScheme.onPrimaryContainer,
//                        fontWeight = FontWeight.Medium
//                    )
//                }
//            }
//        }
        
        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Nearby Devices") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Favorites") }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab content
        when (selectedTab) {
            0 -> NearbyDevicesTab(
                devices = devices,
                isScanning = isScanning,
                directionIndicator = directionIndicator,
                bluetoothScanner = bluetoothScanner,
                onDeviceClick = { device -> selectedDevice = device },
                onRefreshScan = onRefreshScan
            )
            1 -> FavoritesTab(
                bluetoothScanner = bluetoothScanner,
                directionIndicator = directionIndicator,
                onDeviceClick = { device -> selectedDevice = device }
            )
        }
    }
    
    // Device detail dialog
    selectedDevice?.let { device ->
        DeviceDetailDialog(
            device = device,
            directionIndicator = directionIndicator,
            onDismiss = { selectedDevice = null }
        )
    }
}

@Composable
fun NearbyDevicesTab(
    devices: List<BluetoothDevice>,
    isScanning: Boolean,
    directionIndicator: DirectionIndicator,
    bluetoothScanner: BluetoothScanner? = null,
    onDeviceClick: (BluetoothDevice) -> Unit,
    onRefreshScan: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Refresh button - show when devices exist or when not scanning
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    Log.d("MainActivity", "Refresh button clicked")
                    onRefreshScan()
                },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) 
                        MaterialTheme.colorScheme.surfaceVariant 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Scanning..." else "Refresh Scan")
            }
        }
        
        if (devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isScanning) "Scanning for devices..." else "No devices found",
                        color = Color.Gray
                    )
                    if (!isScanning) {
                        Text(
                            text = "Tap 'Scan' to find nearby Bluetooth devices",
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxHeight()
            ) {
                items(devices) { device ->
                    DeviceItem(
                        device = device,
                        directionIndicator = directionIndicator,
                        bluetoothScanner = bluetoothScanner,
                        onDeviceClick = onDeviceClick
                    )
                }
            }
        }
    }
}

@Composable
fun FavoritesTab(
    bluetoothScanner: BluetoothScanner,
    directionIndicator: DirectionIndicator,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    val favoriteDevices by remember { mutableStateOf(bluetoothScanner.getFavoriteDevices()) }
    
            if (favoriteDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No favorite devices",
                    color = Color.Gray
                )
                Text(
                    text = "Tap the star icon on devices to add them to favorites",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxHeight()
        ) {
            items(favoriteDevices) { device ->
                DeviceItem(
                    device = device,
                    directionIndicator = directionIndicator,
                    bluetoothScanner = bluetoothScanner,
                    onDeviceClick = onDeviceClick
                )
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: BluetoothDevice,
    directionIndicator: DirectionIndicator,
    bluetoothScanner: BluetoothScanner? = null,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    val directionArrow = directionIndicator.getDirectionArrow(device.rssi)
    val directionText = when (directionIndicator.getDirectionToDevice(device.rssi)) {
        Direction.VERY_CLOSE -> "Very Close - You're almost on top of it!"
        Direction.CLOSE -> "Close - Look around carefully"
        Direction.MEDIUM -> "Medium distance - Scan the area"
        Direction.FAR -> "Far - Move around to get closer"
    }
    
    val distanceEstimate = DistanceCalculator.estimateDistance(device.rssi)
    val isFavorite = bluetoothScanner?.let { scanner ->
        remember { mutableStateOf(scanner.getFavoriteDevices().any { it.address == device.address }) }
    } ?: remember { mutableStateOf(false) }
    
    // Generate a better device name
    val deviceName = remember(device.name, device.address) {
        when {
            device.name.isNotBlank() && device.name != "Unknown Device" -> device.name
            device.address.startsWith("00:00:00") -> "Smart Device"
            device.address.startsWith("AA:BB:CC") -> "Bluetooth Speaker"
            device.address.startsWith("FF:EE:DD") -> "Wireless Headphones"
            device.address.startsWith("11:22:33") -> "Smart Watch"
            device.address.startsWith("CF:97:B5") -> "Bluetooth Device"
            device.address.startsWith("A4:C6:4F") -> "Smart Speaker"
            device.address.startsWith("B8:27:EB") -> "Raspberry Pi"
            device.address.startsWith("DC:A6:32") -> "Raspberry Pi"
            else -> "Device ${device.address.takeLast(4)}"
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable { onDeviceClick(device) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (distanceEstimate.distance <= 1.0f) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = deviceName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Device type tag
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (device.deviceType == DeviceType.BLE) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ),
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = device.deviceType.shortName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (device.deviceType == DeviceType.BLE) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = device.address,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                
                // Favorite button
                bluetoothScanner?.let { scanner ->
                    IconButton(
                        onClick = {
                            if (isFavorite.value) {
                                scanner.removeFavoriteDevice(device.address)
                                isFavorite.value = false
                            } else {
                                scanner.saveFavoriteDevice(device)
                                isFavorite.value = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite.value) {
                                Icons.Default.Star
                            } else {
                                Icons.Default.StarBorder
                            },
                            contentDescription = if (isFavorite.value) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite.value) Color.Yellow else Color.Gray
                        )
                    }
                }
                
                // Signal strength indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellular4Bar,
                        contentDescription = null,
                        tint = getSignalStrengthColor(device.rssi),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${device.rssi} dBm",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Distance information
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${distanceEstimate.description} (~${distanceEstimate.distance}m)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confidence: ${(distanceEstimate.confidence * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Direction indicator with enhanced guidance
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = directionArrow,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = directionText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                // Enhanced directional guidance
                val directionalGuidance = directionIndicator.getDirectionalGuidance(device.address, device.rssi)
                if (directionalGuidance != "🔄 Move around to determine direction") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = directionalGuidance,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Last seen information
            if (device.lastSeen > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last seen: ${formatTimeAgo(device.lastSeen)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DeviceDetailDialog(
    device: BluetoothDevice,
    directionIndicator: DirectionIndicator,
    onDismiss: () -> Unit
) {
    val distanceEstimate = DistanceCalculator.estimateDistance(device.rssi)
    val direction = directionIndicator.getDirectionToDevice(device.rssi)
    val currentDirection = directionIndicator.currentDirection.collectAsState().value
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Find ${device.name.ifBlank { "Device" }}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Distance info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Distance",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${distanceEstimate.description} (~${distanceEstimate.distance}m)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Direction guidance
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "How to Find It",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        when (direction) {
                            Direction.VERY_CLOSE -> {
                                Text(
                                    text = "📍 You're almost on top of it!",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Look around carefully - it's within 0.5 meters",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Direction.CLOSE -> {
                                Text(
                                    text = "🔍 Look around carefully",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Move slowly in a circle to find the strongest signal",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Direction.MEDIUM -> {
                                Text(
                                    text = "👀 Scan the area",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Walk around and watch for signal strength changes",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Direction.FAR -> {
                                Text(
                                    text = "🔭 Move around to get closer",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Start walking in any direction and monitor signal changes",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Current orientation
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "You're facing ${getDirectionName(currentDirection)}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}

@Composable
fun getSignalStrengthColor(rssi: Int): Color {
    return when {
        rssi >= -50 -> Color.Green
        rssi >= -70 -> Color.Yellow
        rssi >= -90 -> Color(0xFFFFA500)
        else -> Color.Red
    }
}

private fun getDirectionName(degrees: Float): String {
    return when {
        degrees in 315f..45f -> "North"
        degrees in 45f..135f -> "East"
        degrees in 135f..225f -> "South"
        degrees in 225f..315f -> "West"
        else -> "North"
    }
}

