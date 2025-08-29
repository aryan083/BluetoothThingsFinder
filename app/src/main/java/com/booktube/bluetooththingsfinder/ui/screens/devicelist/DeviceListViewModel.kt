package com.booktube.bluetooththingsfinder.ui.screens.devicelist

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booktube.bluetooththingsfinder.bluetooth.BluetoothScanner
import com.booktube.bluetooththingsfinder.bluetooth.BluetoothScanState
import com.booktube.bluetooththingsfinder.model.DeviceType
import com.booktube.bluetooththingsfinder.repository.BluetoothRepository
import com.booktube.bluetooththingsfinder.util.hasRequiredBluetoothPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import com.booktube.bluetooththingsfinder.BluetoothDevice
import com.booktube.bluetooththingsfinder.model.FavoriteDevice

/**
 * UI state for the device list screen.
 */
data class DeviceListUiState(
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val error: String? = null,
    val devices: List<DeviceItemUiState> = emptyList()
)

/**
 * Represents a single device in the UI.
 */
data class DeviceItemUiState(
    val id: String,
    val name: String,
    val address: String,
    val rssi: Int?,
    val deviceType: DeviceType,
    val isFavorite: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * ViewModel for the [DeviceListScreen].
 *
 * @property bluetoothScanner Handles BLE scanning operations
 * @property bluetoothRepository Manages device data and favorites
 */
class DeviceListViewModel(
    private val bluetoothScanner: BluetoothScanner,
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState

    private var isInitialized = false

    init {
        viewModelScope.launch {
            // Observe scan state changes
            bluetoothScanner.scanState.collect { scanState ->
                _uiState.value = _uiState.value.copy(
                    isScanning = scanState is BluetoothScanState.Scanning,
                    error = if (scanState is BluetoothScanState.Error) {
                        scanState.message
                    } else {
                        null
                    }
                )
            }
        }

        viewModelScope.launch {
            // Observe scanned devices
            bluetoothRepository.scannedDevices.collect { devices ->
                updateDeviceList(devices)
            }
        }

        viewModelScope.launch {
            // Observe favorite devices
            bluetoothRepository.favoriteDevices.collect { favorites ->
                updateFavorites(favorites)
            }
        }
    }

    /**
     * Checks and requests necessary permissions for Bluetooth scanning.
     */
    fun checkAndRequestPermissions() {
        if (isInitialized) return
        
        // Check if Bluetooth is supported
        if (bluetoothScanner.bluetoothAdapter == null) {
            _uiState.value = _uiState.value.copy(
                error = "Bluetooth is not supported on this device"
            )
            return
        }

        // Check if Bluetooth is enabled
        if (bluetoothScanner.bluetoothAdapter?.isEnabled != true) {
            _uiState.value = _uiState.value.copy(
                error = "Bluetooth is disabled. Please enable Bluetooth and try again."
            )
            return
        }
        
        isInitialized = true
    }

    /**
     * Starts scanning for nearby Bluetooth devices.
     */
    fun startScanning() {
        if (!isInitialized) {
            checkAndRequestPermissions()
            return
        }
        
        viewModelScope.launch {
            try {
                bluetoothScanner.startScan()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start scanning: ${e.message}"
                )
                Timber.e(e, "Failed to start BLE scan")
            }
        }
    }

    /**
     * Stops the ongoing BLE scan.
     */
    fun stopScanning() {
        viewModelScope.launch {
            try {
                bluetoothScanner.stopScan()
            } catch (e: Exception) {
                Timber.e(e, "Error stopping BLE scan")
            }
        }
    }

    /**
     * Toggles the favorite status of a device.
     *
     * @param deviceId The ID of the device to toggle
     * @param isFavorite Whether the device should be favorited or not
     */
    fun toggleFavorite(deviceId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                if (isFavorite) {
                    bluetoothRepository.removeFromFavorites(deviceId)
                } else {
                    val device = bluetoothRepository.getDevice(deviceId)
                    if (device != null) {
                        bluetoothRepository.addToFavorites(device)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling favorite status for device $deviceId")
            }
        }
    }

    /**
     * Updates the device list with the latest scan results.
     */
    private fun updateDeviceList(devices: List<BluetoothDevice>) {
        val currentDevices = _uiState.value.devices.associateBy { it.id }.toMutableMap()
        
        // Update existing devices or add new ones
        devices.forEach { device ->
            val isFavorite = currentDevices[device.address]?.isFavorite ?: false
            currentDevices[device.address] = DeviceItemUiState(
                id = device.address,
                name = device.name.ifEmpty { "Unknown Device" },
                address = device.address,
                rssi = device.rssi,
                deviceType = device.deviceType,
                isFavorite = isFavorite,
                lastSeen = System.currentTimeMillis()
            )
        }
        
        // Sort by name and then by signal strength (strongest first)
        val sortedDevices = currentDevices.values.sortedWith(
            compareBy(
                { it.name },
                { it.rssi ?: Int.MIN_VALUE }
            )
        )
        
        _uiState.value = _uiState.value.copy(
            devices = sortedDevices,
            isLoading = false
        )
    }

    /**
     * Updates the favorite status of devices in the list.
     */
    private fun updateFavorites(favorites: List<FavoriteDevice>) {
        val favoriteAddresses = favorites.map { it.macAddress }.toSet()
        
        _uiState.value = _uiState.value.copy(
            devices = _uiState.value.devices.map { device ->
                device.copy(isFavorite = device.address in favoriteAddresses)
            }
        )
    }
}
