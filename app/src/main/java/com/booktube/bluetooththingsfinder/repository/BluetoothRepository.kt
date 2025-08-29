package com.booktube.bluetooththingsfinder.repository

import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.le.ScanResult
import com.booktube.bluetooththingsfinder.data.dao.FavoriteDeviceDao
import com.booktube.bluetooththingsfinder.model.DeviceType
import com.booktube.bluetooththingsfinder.model.FavoriteDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository that handles all data operations for Bluetooth devices and favorites.
 * 
 * This class serves as the single source of truth for all data operations in the app.
 * It abstracts the data sources (local database, BLE scanning) from the rest of the app.
 */
class BluetoothRepository(
    private val favoriteDeviceDao: FavoriteDeviceDao,
) {
    // In-memory cache of scanned devices
    private val _scannedDevices = mutableMapOf<String, com.booktube.bluetooththingsfinder.BluetoothDevice>()
    private val _scannedDevicesFlow = MutableStateFlow<List<com.booktube.bluetooththingsfinder.BluetoothDevice>>(emptyList())
    
    /**
     * Flow of scanned devices to observe updates in UI.
     */
    val scannedDevices: Flow<List<com.booktube.bluetooththingsfinder.BluetoothDevice>> = _scannedDevicesFlow.asStateFlow()
    
    /**
     * Get all favorite devices as a Flow that emits updates when the data changes.
     */
    val favoriteDevices: Flow<List<FavoriteDevice>> = favoriteDeviceDao.getAll()
    
    /**
     * Update the list of scanned devices with new scan results.
     * 
     * @param results The new scan results from the BLE scanner
     */
    fun updateScannedDevices(results: List<ScanResult>) {
        results.forEach { result ->
            val device = result.device
            val bluetoothDevice = com.booktube.bluetooththingsfinder.BluetoothDevice(
                name = device.name ?: "Unknown Device",
                address = device.address,
                rssi = result.rssi,
                deviceType = when {
                    device.type == AndroidBluetoothDevice.DEVICE_TYPE_LE -> DeviceType.BLE
                    device.type == AndroidBluetoothDevice.DEVICE_TYPE_CLASSIC -> DeviceType.CLASSIC
                    device.type == AndroidBluetoothDevice.DEVICE_TYPE_DUAL -> DeviceType.DUAL
                    else -> DeviceType.BLE // Default to BLE if type is unknown
                },
                lastSeen = System.currentTimeMillis(),
                bondState = device.bondState
            )
            _scannedDevices[device.address] = bluetoothDevice
        }
        _scannedDevicesFlow.value = _scannedDevices.values.toList()
    }
    
    /**
     * Check if a device with the given MAC address is in favorites.
     * 
     * @param macAddress The MAC address of the device to check
     * @return `true` if the device is in favorites, `false` otherwise
     */
    suspend fun isFavorite(macAddress: String): Boolean {
        return favoriteDeviceDao.isFavorite(macAddress)
    }
    
    /**
     * Add a device to favorites.
     * 
     * @param device The device to add to favorites
     */
    suspend fun addToFavorites(device: com.booktube.bluetooththingsfinder.BluetoothDevice) {
        val favoriteDevice = FavoriteDevice.fromBluetoothDevice(device)
        favoriteDeviceDao.insert(favoriteDevice)
    }
    
    /**
     * Remove a device from favorites.
     * 
     * @param macAddress The MAC address of the device to remove from favorites
     */
    suspend fun removeFromFavorites(macAddress: String) {
        favoriteDeviceDao.deleteByMacAddress(macAddress)
    }
    
    /**
     * Toggle the favorite status of a device.
     * 
     * @param device The device to toggle favorite status for
     * @return `true` if the device is now a favorite, `false` if it was removed
     */
    suspend fun toggleFavorite(device: com.booktube.bluetooththingsfinder.BluetoothDevice): Boolean {
        return if (isFavorite(device.address)) {
            removeFromFavorites(device.address)
            false
        } else {
            addToFavorites(device)
            true
        }
    }
    
    /**
     * Clear all scanned devices from memory.
     */
    fun clearScannedDevices() {
        _scannedDevices.clear()
        _scannedDevicesFlow.value = emptyList()
    }
    
    /**
     * Get a specific device by its MAC address.
     * 
     * @param macAddress The MAC address of the device to retrieve
     * @return The device if found, `null` otherwise
     */
    fun getDevice(macAddress: String): com.booktube.bluetooththingsfinder.BluetoothDevice? {
        return _scannedDevices[macAddress]
    }
}

