package com.booktube.bluetooththingsfinder

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private val _favorites = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val favorites: StateFlow<List<BluetoothDevice>> = _favorites.asStateFlow()

    init {
        // Load favorites initially
        try {
            _favorites.value = deviceStorage.getFavoriteDevices()
        } catch (e: Exception) {
            Log.w("BluetoothScanner", "Failed to load favorites: ${e.message}")
        }
    }
    
    // BroadcastReceiver for Classic Bluetooth device discovery
    private val classicBluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AndroidBluetoothDevice.ACTION_FOUND -> {
                    try {
                        val androidDevice: AndroidBluetoothDevice? = intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                        val rssi = intent.getShortExtra(AndroidBluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        
                        androidDevice?.let { device ->
                            val bluetoothDevice = BluetoothDevice(
                                name = device.name ?: "Unknown Device",
                                address = device.address,
                                rssi = if (rssi == Short.MIN_VALUE.toInt()) -100 else rssi,
                                deviceType = DeviceType.CLASSIC,
                                lastSeen = System.currentTimeMillis(),
                                bluetoothClassMajor = try { device.bluetoothClass?.majorDeviceClass } catch (e: SecurityException) { null },
                                bondState = try { device.bondState } catch (e: SecurityException) { 0 }
                            )
                            
                            addDeviceToList(bluetoothDevice)
                        }
                    } catch (e: Exception) {
                        Log.e("BluetoothScanner", "Error processing classic device: ${e.message}")
                    }
                }
            }
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = BluetoothDevice(
                name = result.device.name ?: "Unknown Device",
                address = result.device.address,
                rssi = result.rssi,
                deviceType = DeviceType.BLE,
                lastSeen = System.currentTimeMillis(),
                // BLE devices often have null/0 class; include if present
                bluetoothClassMajor = try { result.device.bluetoothClass?.majorDeviceClass } catch (e: SecurityException) { null },
                bondState = try { result.device.bondState } catch (e: SecurityException) { 0 }
            )
            
            addDeviceToList(device)
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            _isScanning.value = false
            Log.e("BluetoothScanner", "Scan failed with error code: $errorCode")
        }
    }
    
    // Helper function to add devices to the list and handle common logic
    private fun addDeviceToList(device: BluetoothDevice) {
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
        
        Log.d("BluetoothScanner", "Found ${device.deviceType.shortName} device: ${device.name} (${device.address}) RSSI: ${device.rssi}")
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
            
            // Start BLE scan (primary functionality)
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            bluetoothAdapter.bluetoothLeScanner?.startScan(
                null, // No filters - scan all devices
                scanSettings,
                scanCallback
            )
            
            // Try to add Classic Bluetooth discovery (optional)
            try {
                val filter = IntentFilter(AndroidBluetoothDevice.ACTION_FOUND)
                context.registerReceiver(classicBluetoothReceiver, filter)
                bluetoothAdapter.startDiscovery()
                Log.d("BluetoothScanner", "Started Bluetooth scan (BLE + Classic)")
            } catch (e: SecurityException) {
                Log.w("BluetoothScanner", "Classic Bluetooth discovery not available: ${e.message}")
                Log.d("BluetoothScanner", "Started Bluetooth scan (BLE only)")
            }
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
            
            // Start BLE scan (primary functionality)
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            bluetoothAdapter.bluetoothLeScanner?.startScan(
                null, // No filters - scan all devices
                scanSettings,
                scanCallback
            )
            
            // Try to add Classic Bluetooth discovery (optional)
            try {
                val filter = IntentFilter(AndroidBluetoothDevice.ACTION_FOUND)
                context.registerReceiver(classicBluetoothReceiver, filter)
                bluetoothAdapter.startDiscovery()
                Log.d("BluetoothScanner", "Started continuous Bluetooth scan (BLE + Classic)")
            } catch (e: SecurityException) {
                Log.w("BluetoothScanner", "Classic Bluetooth discovery not available: ${e.message}")
                Log.d("BluetoothScanner", "Started continuous Bluetooth scan (BLE only)")
            }
            
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
                    // Stop and restart BLE scan (primary functionality)
                    bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
                    bluetoothAdapter.bluetoothLeScanner?.startScan(
                        null,
                        ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build(),
                        scanCallback
                    )
                    
                    // Try to restart Classic Bluetooth discovery (optional)
                    try {
                        bluetoothAdapter.cancelDiscovery()
                        bluetoothAdapter.startDiscovery()
                        Log.d("BluetoothScanner", "Re-scanning for devices (BLE + Classic)")
                    } catch (e: SecurityException) {
                        Log.d("BluetoothScanner", "Re-scanning for devices (BLE only)")
                    }
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
            // Stop BLE scan (primary functionality)
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
            
            // Try to stop Classic Bluetooth discovery (optional)
            try {
                bluetoothAdapter.cancelDiscovery()
                context.unregisterReceiver(classicBluetoothReceiver)
                Log.d("BluetoothScanner", "Stopped Bluetooth scan (BLE + Classic)")
            } catch (e: Exception) {
                // Classic Bluetooth wasn't running or receiver wasn't registered
                Log.d("BluetoothScanner", "Stopped Bluetooth scan (BLE only)")
            }
            
            _isScanning.value = false
        } catch (e: Exception) {
            Log.e("BluetoothScanner", "Error stopping scan: ${e.message}")
        }
    }
    
    fun clearDevices() {
        _devices.value = emptyList()
    }
    
    fun getFavoriteDevices(): List<BluetoothDevice> {
        return _favorites.value
    }
    
    fun saveFavoriteDevice(device: BluetoothDevice) {
        // Save a snapshot (avoid live-changing RSSI/lastSeen)
        val snapshot = device.copy(
            rssi = 0,
            lastSeen = 0
        )
        deviceStorage.saveFavoriteDevice(snapshot)
        // Update flow
        _favorites.value = deviceStorage.getFavoriteDevices()
    }
    
    fun removeFavoriteDevice(deviceAddress: String) {
        deviceStorage.removeFavoriteDevice(deviceAddress)
        // Update flow
        _favorites.value = deviceStorage.getFavoriteDevices()
    }
    
    fun getDeviceHistory(): List<BluetoothDevice> {
        return deviceStorage.getDeviceHistory()
    }
}
