package com.booktube.bluetooththingsfinder.ui.screens.devicelist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.SettingsBluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import org.koin.androidx.compose.koinViewModel
import com.booktube.bluetooththingsfinder.R
import com.booktube.bluetooththingsfinder.model.DeviceType
import com.booktube.bluetooththingsfinder.ui.components.DeviceIcon
import com.booktube.bluetooththingsfinder.ui.components.LoadingIndicator
import com.booktube.bluetooththingsfinder.ui.theme.spacing

/**
 * Screen that displays a list of discovered Bluetooth devices.
 *
 * @param onDeviceClick Callback when a device is clicked
 * @param onNavigateToSettings Callback when the settings button is clicked
 * @param viewModel The ViewModel that provides the data and business logic
 */
@Composable
fun DeviceListScreen(
    onDeviceClick: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DeviceListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Request location permission when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.checkAndRequestPermissions()
    }
    
    // Start scanning when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }
    
    // Stop scanning when the screen is destroyed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.isScanning) {
                        viewModel.stopScanning()
                    } else {
                        viewModel.startScanning()
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isScanning) {
                        Icons.Default.Bluetooth
                    } else {
                        Icons.Default.BluetoothSearching
                    },
                    contentDescription = if (uiState.isScanning) {
                        stringResource(R.string.stop_scanning)
                    } else {
                        stringResource(R.string.start_scanning)
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.error?.isNotEmpty() == true -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.startScanning() }
                    )
                }
                uiState.devices.isEmpty() -> {
                    EmptyState(
                        isScanning = uiState.isScanning,
                        onStartScan = { viewModel.startScanning() }
                    )
                }
                else -> {
                    DeviceList(
                        devices = uiState.devices,
                        onDeviceClick = onDeviceClick,
                        onToggleFavorite = { deviceId, isFavorite ->
                            viewModel.toggleFavorite(deviceId, isFavorite)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Displays a list of Bluetooth devices.
 */
@Composable
private fun DeviceList(
    devices: List<DeviceItemUiState>,
    onDeviceClick: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.small)
    ) {
        items(devices, key = { it.id }) { device ->
            DeviceItem(
                device = device,
                onClick = { onDeviceClick(device.id) },
                onToggleFavorite = { isFavorite ->
                    onToggleFavorite(device.id, isFavorite)
                }
            )
            Divider()
        }
    }
}

/**
 * Displays a single Bluetooth device item in the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceItem(
    device: DeviceItemUiState,
    onClick: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
            .padding(vertical = MaterialTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon based on type
            DeviceIcon(
                deviceType = device.deviceType,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            
            // Device info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (device.rssi != null) {
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            // Favorite button
            IconButton(
                onClick = { onToggleFavorite(!device.isFavorite) },
                modifier = Modifier.padding(start = MaterialTheme.spacing.small)
            ) {
                Icon(
                    imageVector = if (device.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = if (device.isFavorite) {
                        stringResource(R.string.remove_from_favorites)
                    } else {
                        stringResource(R.string.add_to_favorites)
                    },
                    tint = if (device.isFavorite) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Displays an error state with a retry button.
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

/**
 * Displays an empty state with a message and a button to start scanning.
 */
@Composable
private fun EmptyState(
    isScanning: Boolean,
    onStartScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        
        Text(
            text = if (isScanning) {
                stringResource(R.string.no_devices_found)
            } else {
                stringResource(R.string.press_to_scan)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        if (!isScanning) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            
            Button(onClick = onStartScan) {
                Text(stringResource(R.string.start_scanning))
            }
        }
    }
}
