package com.booktube.bluetooththingsfinder.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.booktube.bluetooththingsfinder.BluetoothDevice
import com.booktube.bluetooththingsfinder.model.DeviceType
import com.booktube.bluetooththingsfinder.repository.BluetoothRepository
import com.booktube.bluetooththingsfinder.util.hasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages Bluetooth Low Energy (BLE) scanning operations.
 * 
 * This class handles the scanning of nearby BLE devices, manages scan states,
 * and provides updates through StateFlow. It also handles necessary permissions
 * and Bluetooth state checks.
 */
class BluetoothScanner(
    private val context: Context,
    private val bluetoothRepository: BluetoothRepository
) {
    private val _scanState = MutableStateFlow<BluetoothScanState>(BluetoothScanState.Stopped)
    val scanState: StateFlow<BluetoothScanState> = _scanState.asStateFlow()
    
    val bluetoothAdapter: BluetoothAdapter?
        get() = _bluetoothAdapter
    private var _bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: BluetoothScanCallback? = null
    private var scanJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Scan period in milliseconds (5 minutes)
    private val SCAN_PERIOD: Long = 5 * 60 * 1000
    
    init {
        initializeBluetooth()
    }
    
    /**
     * Initialize Bluetooth adapter and scanner.
     */
    private fun initializeBluetooth() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        _bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = _bluetoothAdapter?.bluetoothLeScanner
        
        if (_bluetoothAdapter == null || bluetoothLeScanner == null) {
            _scanState.value = BluetoothScanState.Error("Bluetooth is not supported on this device")
        } else if (!_bluetoothAdapter!!.isEnabled) {
            _scanState.value = BluetoothScanState.BluetoothDisabled
        } else {
            _scanState.value = BluetoothScanState.Stopped
        }
    }
    
    /**
     * Start scanning for BLE devices.
     * 
     * This will check for necessary permissions and Bluetooth state before starting the scan.
     * 
     * @param scanPeriodMs Optional custom scan period in milliseconds. If not provided, uses default SCAN_PERIOD.
     */
    @SuppressLint("MissingPermission")
    fun startScan(scanPeriodMs: Long = SCAN_PERIOD) {
        if (!hasRequiredPermissions()) {
            _scanState.value = BluetoothScanState.PermissionRequired
            return
        }
        
        if (_bluetoothAdapter == null || !_bluetoothAdapter!!.isEnabled) {
            _scanState.value = BluetoothScanState.BluetoothDisabled
            return
        }
        
        if (_scanState.value is BluetoothScanState.Scanning) {
            // Already scanning
            return
        }
        
        _scanState.value = BluetoothScanState.Scanning
        bluetoothRepository.clearScannedDevices()
        
        // Create a new scan callback if needed
        if (scanCallback == null) {
            scanCallback = BluetoothScanCallback { result ->
                bluetoothRepository.updateScannedDevices(listOf(result))
            }
        }
        
        // Start the scan
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                bluetoothLeScanner?.startScan(scanCallback)
                
                // Stop scanning after the scan period
                handler.postDelayed({
                    stopScan()
                }, scanPeriodMs)
                
            } catch (e: Exception) {
                Timber.e(e, "Error starting BLE scan")
                _scanState.value = BluetoothScanState.Error("Failed to start scan: ${e.message}")
            }
        }
    }
    
    /**
     * Stop the ongoing BLE scan.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (_scanState.value !is BluetoothScanState.Scanning) {
            return
        }
        
        scanJob?.cancel()
        scanJob = null
        
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            _scanState.value = BluetoothScanState.Stopped
        } catch (e: Exception) {
            Timber.e(e, "Error stopping BLE scan")
            _scanState.value = BluetoothScanState.Error("Failed to stop scan: ${e.message}")
        }
    }
    
    /**
     * Toggle the BLE scan state.
     * If scanning is in progress, it will be stopped. Otherwise, a new scan will be started.
     * 
     * @param scanPeriodMs Optional custom scan period in milliseconds if starting a new scan.
     */
    fun toggleScan(scanPeriodMs: Long = SCAN_PERIOD) {
        when (_scanState.value) {
            is BluetoothScanState.Scanning -> stopScan()
            else -> startScan(scanPeriodMs)
        }
    }
    
    /**
     * Check if the app has all the required permissions for BLE scanning.
     * 
     * @return `true` if all required permissions are granted, `false` otherwise.
     */
    private fun hasRequiredPermissions(): Boolean {
        return context.hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
               context.hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
               context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    /**
     * Clean up resources when the scanner is no longer needed.
     */
    fun cleanup() {
        stopScan()
        scanCallback = null
    }
    
    /**
     * Callback for BLE scan results.
     */
    private class BluetoothScanCallback(
        private val onDeviceFound: (ScanResult) -> Unit
    ) : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            onDeviceFound(result)
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { onDeviceFound(it) }
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature not supported"
                else -> "Unknown error: $errorCode"
            }
            Timber.e("BLE scan failed: $errorMessage")
        }
    }
}

/**
 * Represents the current state of the Bluetooth scanner.
 */
sealed class BluetoothScanState {
    /** Scanner is currently stopped */
    object Stopped : BluetoothScanState()
    
    /** Scanner is currently scanning for devices */
    object Scanning : BluetoothScanState()
    
    /** Bluetooth is disabled on the device */
    object BluetoothDisabled : BluetoothScanState()
    
    /** Required permissions are not granted */
    object PermissionRequired : BluetoothScanState()
    
    /** An error occurred during scanning */
    data class Error(val message: String) : BluetoothScanState()
}
