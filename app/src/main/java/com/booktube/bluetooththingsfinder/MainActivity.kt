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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabPosition
import androidx.compose.ui.text.style.TextAlign

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
        bluetoothScanner = BluetoothScanner(this, bluetoothAdapter)
        directionIndicator = DirectionIndicator(this)
        
        // Connect the direction indicator to the scanner for enhanced tracking
        bluetoothScanner.setDirectionIndicator(directionIndicator)
        
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
                        onStopScan = { bluetoothScanner.stopScan() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        directionIndicator.start()
        // Start continuous scanning automatically
        checkPermissionsAndStartScan()
    }
    
    override fun onPause() {
        super.onPause()
        directionIndicator.stop()
        // Keep scanning running in background
        // bluetoothScanner.stopScan() // Removed to keep continuous scanning
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
    onStopScan: () -> Unit
) {
    val devices by bluetoothScanner.devices.collectAsState()
    val isScanning by bluetoothScanner.isScanning.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Modern top header with subtle elevation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // App title with better typography
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bluetooth Things Finder",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Help button with better styling
                    Surface(
                        modifier = Modifier.clickable { showHelpDialog = true },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "Help",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp).size(20.dp)
                        )
                    }
                }
                
                // Enhanced status indicator with better visual design
                if (isScanning) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Animated scanning indicator
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BluetoothSearching,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Scanning for devices",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Move around to discover nearby Bluetooth devices",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Device count with better styling
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${devices.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Modern tabs with better styling
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) { 
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Nearby Devices",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                            }
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) { 
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Favorites",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        // Tab content with better spacing
        when (selectedTab) {
            0 -> NearbyDevicesTab(
                devices = devices,
                isScanning = isScanning,
                directionIndicator = directionIndicator,
                bluetoothScanner = bluetoothScanner,
                onDeviceClick = { device -> selectedDevice = device }
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
    
    // Help dialog
    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }
}

@Composable
fun NearbyDevicesTab(
    devices: List<BluetoothDevice>,
    isScanning: Boolean,
    directionIndicator: DirectionIndicator,
    bluetoothScanner: BluetoothScanner? = null,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    if (devices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Enhanced empty state icon with better styling
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isScanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = if (isScanning) "Looking for devices..." else "No devices found",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isScanning) 
                        "Move around to discover nearby Bluetooth devices" 
                    else 
                        "Make sure Bluetooth is enabled and devices are nearby",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                // Additional helpful tip
                if (!isScanning) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "💡 Tip: Try moving to different rooms or areas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
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
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Enhanced empty state icon with better styling
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "No favorite devices",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Tap the star icon on devices to add them to your favorites",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                
                // Additional helpful tip
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "💡 Tip: Favorites are saved between app sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
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
    val distanceEstimate = DistanceCalculator.estimateDistance(device.rssi)
    val isFavorite = bluetoothScanner?.let { scanner ->
        remember { mutableStateOf(scanner.getFavoriteDevices().any { it.address == device.address }) }
    } ?: remember { mutableStateOf(false) }
    
    // Get movement direction for this device
    val movementDirection = bluetoothScanner?.let { scanner ->
        remember { mutableStateOf(scanner.getDeviceMovementDirection(device.address)) }
    } ?: remember { mutableStateOf<MovementDirection?>(null) }
    
    // Generate a better device name
    val deviceName = remember(device.name, device.address) {
        when {
            device.name.isNotBlank() && device.name != "Unknown Device" -> device.name
            else -> "Device ${device.address.takeLast(4)}"
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDeviceClick(device) },
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced device icon with better styling
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = device.deviceType.icon,
                        fontSize = 24.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            // Enhanced device info with better typography
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Device type badge with modern styling
                    Text(
                        text = device.deviceType.shortName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                
                // Device address with subtle styling
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Enhanced distance and movement info with better layout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    // Direction indicator with better styling
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = directionIndicator.getDirectionArrow(device.rssi),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Distance information
                    Text(
                        text = distanceEstimate.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Movement indicator with better styling
                    movementDirection?.value?.let { movement ->
                        Spacer(modifier = Modifier.width(12.dp))
                        val (movementIcon, movementColor) = when (movement) {
                            MovementDirection.GETTING_CLOSER -> "✅" to Color(0xFF4CAF50)
                            MovementDirection.GETTING_FARTHER -> "❌" to Color(0xFFF44336)
                            MovementDirection.STABLE -> "⏸️" to Color(0xFFFF9800)
                            MovementDirection.UNKNOWN -> "❓" to Color(0xFF9E9E9E)
                        }
                        
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = movementColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = movementIcon,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            
            // Enhanced favorite button with better styling
            bluetoothScanner?.let { scanner ->
                Surface(
                    modifier = Modifier.clickable {
                        if (isFavorite.value) {
                            scanner.removeFavoriteDevice(device.address)
                            isFavorite.value = false
                        } else {
                            scanner.saveFavoriteDevice(device)
                            isFavorite.value = true
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    color = if (isFavorite.value) {
                        Color(0xFFFFD700).copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isFavorite.value) Color(0xFFFFD700).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Icon(
                        imageVector = if (isFavorite.value) {
                            Icons.Default.Star
                        } else {
                            Icons.Default.StarBorder
                        },
                        contentDescription = if (isFavorite.value) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite.value) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }
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
    
    // Get enhanced guidance for this device
    val deviceGuidance = remember(device.address, device.rssi) {
        directionIndicator.getDetailedGuidance(device.address, device.rssi)
    }
    
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
                // Distance info with enhanced guidance
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
                        
                        deviceGuidance?.let { guidance ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = directionIndicator.getDirectionArrow(device.rssi),
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confidence: ${(guidance.confidence * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enhanced guidance section
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
                        
                        deviceGuidance?.let { guidance ->
                            // Movement direction indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                val movementIcon = when (guidance.movementDirection) {
                                    MovementDirection.GETTING_CLOSER -> "✅"
                                    MovementDirection.GETTING_FARTHER -> "❌"
                                    MovementDirection.STABLE -> "⏸️"
                                    MovementDirection.UNKNOWN -> "❓"
                                }
                                Text(
                                    text = movementIcon,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (guidance.movementDirection) {
                                        MovementDirection.GETTING_CLOSER -> "Getting closer - keep going!"
                                        MovementDirection.GETTING_FARTHER -> "Moving away - try different direction"
                                        MovementDirection.STABLE -> "Signal stable - look around"
                                        MovementDirection.UNKNOWN -> "Movement unclear - keep moving"
                                    },
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            
                            // Detailed suggestion
                            Text(
                                text = guidance.suggestion,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        } ?: run {
                            // Fallback to basic guidance
                            val direction = directionIndicator.getDirectionToDevice(device.rssi)
                            when (direction) {
                                Direction.VERY_CLOSE -> {
                                    Text(
                                        text = "🎯 You're almost on top of it!",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Look around carefully - it's within 0.3 meters",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Direction.EXTREMELY_CLOSE -> {
                                    Text(
                                        text = "📍 It's right here!",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Check your immediate surroundings",
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
                                Direction.MEDIUM_CLOSE -> {
                                    Text(
                                        text = "👀 Scan the nearby area",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Walk around and watch for signal changes",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Direction.MEDIUM -> {
                                    Text(
                                        text = "🔭 Scan the area",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Walk around and watch for signal strength changes",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Direction.MEDIUM_FAR -> {
                                    Text(
                                        text = "🔎 Search wider area",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Move around to get a better signal",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Direction.FAR -> {
                                    Text(
                                        text = "🏃 Move around to get closer",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Start walking in any direction and monitor signal changes",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Direction.VERY_FAR -> {
                                    Text(
                                        text = "🚶 Start walking",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "The device is quite far. Start moving in any direction.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
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



