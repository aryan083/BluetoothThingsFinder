package com.booktube.bluetooththingsfinder

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothScanner(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) {
    private val _devices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val devices: StateFlow<List<BluetoothDevice>> = _devices.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val deviceStorage = DeviceStorage(context)
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = BluetoothDevice(
                name = result.device.name ?: "Unknown Device",
                address = result.device.address,
                rssi = result.rssi
            )
            
            // Save to history
            deviceStorage.addToHistory(device)
            
            val currentDevices = _devices.value.toMutableList()
            val existingIndex = currentDevices.indexOfFirst { it.address == device.address }
            val isNewDevice = existingIndex == -1
            
            if (existingIndex != -1) {
                // Update existing device with new RSSI
                currentDevices[existingIndex] = device
            } else {
                // Add new device
                currentDevices.add(device)
                
                // Vibrate for new devices
                if (isNewDevice) {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.let { v ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(100)
                        }
                    }
                }
            }
            
            // Sort by signal strength (RSSI) - stronger signals first
            currentDevices.sortByDescending { it.rssi }
            _devices.value = currentDevices
            
            Log.d("BluetoothScanner", "Found device: ${device.name} (${device.address}) RSSI: ${device.rssi}")
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _isScanning.value = false
            Log.e("BluetoothScanner", "Scan failed with error code: $errorCode")
        }
    }
    
    fun startScan() {
        if (_isScanning.value) return
        
        if (!bluetoothAdapter.isEnabled) {
            Log.w("BluetoothScanner", "Bluetooth is not enabled")
            return
        }
        
        try {
            _isScanning.value = true
            _devices.value = emptyList() // Clear previous results
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            bluetoothAdapter.bluetoothLeScanner?.startScan(
                null, // No filters - scan all devices
                scanSettings,
                scanCallback
            )
            
            Log.d("BluetoothScanner", "Started Bluetooth scan")
        } catch (e: Exception) {
            Log.e("BluetoothScanner", "Error starting scan: ${e.message}")
            _isScanning.value = false
        }
    }
    
    fun startContinuousScan() {
        if (_isScanning.value) return
        
        if (!bluetoothAdapter.isEnabled) {
            Log.w("BluetoothScanner", "Bluetooth is not enabled")
            return
        }
        
        try {
            _isScanning.value = true
            _devices.value = emptyList() // Clear previous results
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            bluetoothAdapter.bluetoothLeScanner?.startScan(
                null, // No filters - scan all devices
                scanSettings,
                scanCallback
            )
            
            Log.d("BluetoothScanner", "Started continuous Bluetooth scan")
            
            // Schedule periodic re-scanning to keep the scan active
            scheduleRescan()
        } catch (e: Exception) {
            Log.e("BluetoothScanner", "Error starting continuous scan: ${e.message}")
            _isScanning.value = false
        }
    }
    
    private fun scheduleRescan() {
        // Re-scan every 30 seconds to keep the scan active
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_isScanning.value) {
                try {
                    bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
                    bluetoothAdapter.bluetoothLeScanner?.startScan(
                        null,
                        ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build(),
                        scanCallback
                    )
                    Log.d("BluetoothScanner", "Re-scanning for devices")
                    scheduleRescan() // Schedule next re-scan
                } catch (e: Exception) {
                    Log.e("BluetoothScanner", "Error during re-scan: ${e.message}")
                }
            }
        }, 30000) // 30 seconds
    }
    
    fun stopScan() {
        if (!_isScanning.value) return
        
        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
            _isScanning.value = false
            Log.d("BluetoothScanner", "Stopped Bluetooth scan")
        } catch (e: Exception) {
            Log.e("BluetoothScanner", "Error stopping scan: ${e.message}")
        }
    }
    
    fun clearDevices() {
        _devices.value = emptyList()
    }
    
    fun getFavoriteDevices(): List<BluetoothDevice> {
        return deviceStorage.getFavoriteDevices()
    }
    
    fun saveFavoriteDevice(device: BluetoothDevice) {
        deviceStorage.saveFavoriteDevice(device)
    }
    
    fun removeFavoriteDevice(deviceAddress: String) {
        deviceStorage.removeFavoriteDevice(deviceAddress)
    }
    
    fun getDeviceHistory(): List<BluetoothDevice> {
        return deviceStorage.getDeviceHistory()
    }
}
