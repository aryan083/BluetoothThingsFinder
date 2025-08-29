package com.mpc.experinment_8

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {
    
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var deviceAdapter: BluetoothDeviceAdapter
    private lateinit var btnScanDevices: MaterialButton
    private lateinit var btnClearDevices: MaterialButton
    private lateinit var tvStatus: TextView
    private lateinit var tvDeviceCount: TextView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var emptyStateLayout: LinearLayout
    
    private var isScanning = false
    private var scanTimeoutHandler: android.os.Handler? = null
    private val SCAN_TIMEOUT_MS = 12000L // 12 seconds timeout
    private var bluetoothReadyHandler: android.os.Handler? = null
    private val BLUETOOTH_READY_CHECK_INTERVAL = 1000L // Check every 1 second
    
    // Request code for enabling Bluetooth
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth enabled!", Toast.LENGTH_SHORT).show()
            updateStatus("Bluetooth enabled - Ready to scan", android.R.drawable.ic_dialog_info)
            // Check paired devices for debugging
            checkPairedDevices()
            // Don't automatically start scanning, let user choose when to scan
        } else {
            Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_LONG).show()
            updateStatus("Bluetooth disabled", android.R.drawable.ic_dialog_alert)
        }
        updatePermissionStatus()
    }
    
    // Request permissions launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.values.all { it }
        if (allPermissionsGranted) {
            // Now that permissions are granted, try to enable Bluetooth if it's not already enabled
            if (!bluetoothAdapter.isEnabled) {
                Toast.makeText(this, "Permissions granted! Now enabling Bluetooth...", Toast.LENGTH_SHORT).show()
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            } else {
                Toast.makeText(this, "All permissions granted! Ready to scan.", Toast.LENGTH_SHORT).show()
                updateStatus("Ready to scan for Bluetooth devices", android.R.drawable.ic_dialog_info)
            }
        } else {
            val deniedPermissions = permissions.filterValues { !it }.keys
            val permissionNames = deniedPermissions.map { permission ->
                when (permission) {
                    Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
                    Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth Scan"
                    Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth Connect"
                    else -> "Unknown"
                }
            }
            val message = "Denied: ${permissionNames.joinToString(", ")}. Please grant in Settings."
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            updateStatus("Permissions denied - Check Settings", android.R.drawable.ic_dialog_alert)
            
            // Check if any permission is permanently denied (should not show rationale)
            val permanentlyDenied = deniedPermissions.any { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }
            
            if (permanentlyDenied) {
                showSettingsDialog()
            }
        }
        updatePermissionStatus()
    }
    
    // Broadcast receiver for Bluetooth device discovery
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    
                    device?.let {
                        val deviceName = if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            it.name ?: "Unknown Device"
                        } else {
                            "Unknown Device"
                        }
                        
                        // Extract RSSI value if available
                        val rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()
                        }
                        
                        // Extract device class if available
                        val deviceClass = if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            it.bluetoothClass
                        } else {
                            null
                        }
                        
                        // Extract bond state if available
                        val bondState = if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            it.bondState
                        } else {
                            0
                        }
                        
                        // Log device discovery for debugging
                        android.util.Log.d("BluetoothDebug", "Found device: $deviceName (${it.address}), RSSI: $rssi, Class: $deviceClass, Bond: $bondState")
                        
                        val bluetoothDevice = BluetoothDeviceItem(
                            name = deviceName,
                            address = it.address,
                            rssi = rssi,
                            deviceClass = deviceClass,
                            bondState = bondState
                        )
                        
                        deviceAdapter.addDevice(bluetoothDevice)
                        updateDeviceCount()
                        updateEmptyState()
                        updateStatus("Found ${deviceAdapter.getDevices().size} devices", android.R.drawable.ic_search_category_default)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    android.util.Log.d("BluetoothDebug", "Discovery started")
                    isScanning = true
                    btnScanDevices.text = "Stop Scanning"
                    showProgress(true)
                    updateStatus("Scanning for devices...", android.R.drawable.ic_search_category_default)
                    updateEmptyState()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    android.util.Log.d("BluetoothDebug", "Discovery finished")
                    isScanning = false
                    showProgress(false)
                    val deviceCount = deviceAdapter.getDevices().size
                    updateStatus(
                        if (deviceCount > 0) "Found $deviceCount devices" else "No devices found",
                        if (deviceCount > 0) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert
                    )
                    updateEmptyState()
                    // Update button text based on current state
                    updatePermissionStatus()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    }
                    
                    val previousState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR)
                    }
                    
                    android.util.Log.d("BluetoothDebug", "Bluetooth state changed from $previousState to $state")
                    
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            updateStatus("Bluetooth ready - Ready to scan", android.R.drawable.ic_dialog_info)
                            updatePermissionStatus()
                            // Check paired devices for debugging
                            checkPairedDevices()
                            
                            // If user was trying to scan but Bluetooth wasn't ready, offer to retry
                            if (btnScanDevices.text == "Scan Devices") {
                                Toast.makeText(this@MainActivity, "Bluetooth is now ready! You can start scanning.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            updateStatus("Bluetooth turned off", android.R.drawable.ic_dialog_alert)
                            updatePermissionStatus()
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            updateStatus("Bluetooth turning on...", android.R.drawable.ic_dialog_info)
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            updateStatus("Bluetooth turning off...", android.R.drawable.ic_dialog_info)
                        }
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initViews()
        initBluetooth()
        setupRecyclerView()
        setupClickListeners()
        
        // Check initial permission status
        updatePermissionStatus()
        
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }
    
    private fun initViews() {
        btnScanDevices = findViewById(R.id.btnScanDevices)
        btnClearDevices = findViewById(R.id.btnClearDevices)
        tvStatus = findViewById(R.id.tvStatus)
        tvDeviceCount = findViewById(R.id.tvDeviceCount)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        recyclerView = findViewById(R.id.recyclerViewDevices)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        progressIndicator = findViewById(R.id.progressIndicator)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
    }
    
    private fun initBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        // Check if Bluetooth is supported on this device
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show()
            updateStatus("Bluetooth not supported", android.R.drawable.ic_dialog_alert)
            btnScanDevices.isEnabled = false
            return
        }
        
        if (bluetoothAdapter.isEnabled) {
            updateStatus("Ready to scan for Bluetooth devices", android.R.drawable.ic_dialog_info)
            // Check paired devices for debugging
            checkPairedDevices()
        } else {
            // Check permissions before trying to enable Bluetooth
            if (checkBluetoothPermissions()) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
                updateStatus("Bluetooth is disabled", android.R.drawable.ic_dialog_alert)
            } else {
                // Request permissions first
                requestBluetoothPermissions()
                updateStatus("Permissions required", android.R.drawable.ic_dialog_alert)
            }
        }
    }
    
    private fun setupRecyclerView() {
        deviceAdapter = BluetoothDeviceAdapter(
            onDeviceClick = { device ->
                showDeviceDetails(device)
            },
            onConnectClick = { device ->
                connectToDevice(device)
            }
        )
        
        recyclerView.apply {
            adapter = deviceAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }
        
        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            if (!isScanning) {
                checkPermissionsAndStartScan()
            }
            swipeRefreshLayout.isRefreshing = false
        }
        
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary)
        )
    }
    
    private fun setupClickListeners() {
        btnScanDevices.setOnClickListener {
            if (isScanning) {
                stopBluetoothScan()
            } else {
                when (btnScanDevices.text.toString()) {
                    "Grant Permissions" -> {
                        if (shouldShowSettingsPrompt()) {
                            showSettingsDialog()
                        } else {
                            requestBluetoothPermissions()
                        }
                    }
                    "Open Settings" -> {
                        openAppSettings()
                    }
                    "Enable Bluetooth" -> {
                        if (checkBluetoothPermissions()) {
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            enableBluetoothLauncher.launch(enableBtIntent)
                        } else {
                            requestBluetoothPermissions()
                        }
                    }
                    "Scan Devices" -> {
                        checkPermissionsAndStartScan()
                    }
                    else -> {
                        // Fallback to original logic
                        if (bluetoothAdapter.isEnabled) {
                            checkPermissionsAndStartScan()
                        } else {
                            if (checkBluetoothPermissions()) {
                                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                enableBluetoothLauncher.launch(enableBtIntent)
                            } else {
                                requestBluetoothPermissions()
                            }
                        }
                    }
                }
            }
        }
        
        btnClearDevices.setOnClickListener {
            clearDeviceList()
        }
        
        // Add a refresh button for paired devices
        findViewById<MaterialButton>(R.id.btnRefreshPaired)?.setOnClickListener {
            if (!::bluetoothAdapter.isInitialized) {
                Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!bluetoothAdapter.isEnabled) {
                if (checkBluetoothPermissions()) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                } else {
                    requestBluetoothPermissions()
                }
                return@setOnClickListener
            }
            
            // Check if we have the necessary permissions
            if (checkBluetoothPermissions()) {
                clearDeviceList()
                checkPairedDevices()
                Toast.makeText(this, "Refreshing paired devices list...", Toast.LENGTH_SHORT).show()
            } else {
                requestBluetoothPermissions()
            }
        }
    }
    
    private fun checkBluetoothPermissions(): Boolean {
        // Check location permission (required for Bluetooth scanning)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        
        // Check Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        
        return true
    }
    
    private fun logBluetoothState() {
        if (::bluetoothAdapter.isInitialized) {
            val state = when (bluetoothAdapter.state) {
                BluetoothAdapter.STATE_OFF -> "OFF"
                BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
                BluetoothAdapter.STATE_ON -> "ON"
                BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
                else -> "UNKNOWN"
            }
            
            val isDiscovering = bluetoothAdapter.isDiscovering
            val isEnabled = bluetoothAdapter.isEnabled
            
            android.util.Log.d("BluetoothDebug", "Adapter State: $state, Enabled: $isEnabled, Discovering: $isDiscovering")
        }
    }
    
    private fun checkPairedDevices() {
        if (::bluetoothAdapter.isInitialized && bluetoothAdapter.isEnabled) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                val pairedDevices = bluetoothAdapter.bondedDevices
                android.util.Log.d("BluetoothDebug", "Paired devices count: ${pairedDevices.size}")
                
                // Add paired devices to the list so users can see them
                pairedDevices.forEach { device ->
                    android.util.Log.d("BluetoothDebug", "Paired device: ${device.name} (${device.address})")
                    
                    val deviceName = device.name ?: "Unknown Device"
                    val bluetoothDevice = BluetoothDeviceItem(
                        name = deviceName,
                        address = device.address,
                        rssi = 0, // Paired devices don't have RSSI
                        deviceClass = device.bluetoothClass,
                        bondState = device.bondState
                    )
                    
                    deviceAdapter.addDevice(bluetoothDevice)
                }
                
                // Update UI to show paired devices
                updateDeviceCount()
                updateEmptyState()
                updateStatus("Found ${pairedDevices.size} paired devices", android.R.drawable.ic_dialog_info)
            }
        }
    }
    
    private fun waitForBluetoothReady(onReady: () -> Unit) {
        if (bluetoothAdapter.state == BluetoothAdapter.STATE_ON) {
            // Bluetooth is already ready
            onReady()
            return
        }
        
        updateStatus("Waiting for Bluetooth to be ready...", android.R.drawable.ic_dialog_info)
        Toast.makeText(this, "Bluetooth is initializing, please wait...", Toast.LENGTH_SHORT).show()
        
        // Clear any existing handler
        bluetoothReadyHandler?.removeCallbacksAndMessages(null)
        
        bluetoothReadyHandler = android.os.Handler(android.os.Looper.getMainLooper())
        bluetoothReadyHandler?.postDelayed(object : Runnable {
            override fun run() {
                if (bluetoothAdapter.state == BluetoothAdapter.STATE_ON) {
                    // Bluetooth is now ready
                    android.util.Log.d("BluetoothDebug", "Bluetooth is now ready, proceeding with scan")
                    updateStatus("Bluetooth ready - Starting scan...", android.R.drawable.ic_dialog_info)
                    onReady()
                } else {
                    // Still not ready, check again
                    android.util.Log.d("BluetoothDebug", "Bluetooth still not ready, state: ${bluetoothAdapter.state}")
                    bluetoothReadyHandler?.postDelayed(this, BLUETOOTH_READY_CHECK_INTERVAL)
                }
            }
        }, BLUETOOTH_READY_CHECK_INTERVAL)
    }
    
    private fun requestBluetoothPermissions() {
        val requiredPermissions = mutableListOf<String>()
        
        // Check location permission (required for Bluetooth scanning)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        // Check Bluetooth permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        
        if (requiredPermissions.isNotEmpty()) {
            // Check if we should show rationale for any permission
            val shouldShowRationale = requiredPermissions.any { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            }
            
            if (shouldShowRationale) {
                showPermissionRationaleDialog(requiredPermissions)
            } else {
                Toast.makeText(this, "Granting permissions for Bluetooth functionality...", Toast.LENGTH_SHORT).show()
                requestPermissionsLauncher.launch(requiredPermissions.toTypedArray())
            }
        }
    }
    
    private fun showPermissionRationaleDialog(permissions: List<String>) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs Bluetooth and Location permissions to scan for nearby Bluetooth devices. Please grant these permissions to continue.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                requestPermissionsLauncher.launch(permissions.toTypedArray())
            }
            .setNegativeButton("Cancel") { _, _ ->
                updateStatus("Permissions required", android.R.drawable.ic_dialog_alert)
            }
            .show()
    }
    
    private fun showSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Some permissions are permanently denied. Please go to Settings > Apps > This App > Permissions to grant the required permissions.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { _, _ ->
                updateStatus("Permissions required", android.R.drawable.ic_dialog_alert)
            }
            .show()
    }
    
    private fun shouldShowSettingsPrompt(): Boolean {
        val missingPermissions = getMissingPermissions()
        return missingPermissions.any { permission ->
            when (permission) {
                "Location" -> !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                "Bluetooth Scan" -> !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_SCAN)
                "Bluetooth Connect" -> !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_CONNECT)
                else -> false
            }
        }
    }
    
    private fun checkPermissionsAndStartScan() {
        if (checkBluetoothPermissions()) {
            startBluetoothScan()
        } else {
            requestBluetoothPermissions()
        }
    }
    
    private fun startBluetoothScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth scan permission not granted", Toast.LENGTH_SHORT).show()
            updatePermissionStatus()
            return
        }
        
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
            updateStatus("Bluetooth is disabled", android.R.drawable.ic_dialog_alert)
            return
        }
        
        // Check if Bluetooth adapter is fully ready (not turning on/off)
        if (bluetoothAdapter.state != BluetoothAdapter.STATE_ON) {
            val stateText = when (bluetoothAdapter.state) {
                BluetoothAdapter.STATE_OFF -> "OFF"
                BluetoothAdapter.STATE_TURNING_OFF -> "TURNING_OFF"
                BluetoothAdapter.STATE_TURNING_ON -> "TURNING_ON"
                BluetoothAdapter.STATE_ON -> "ON"
                else -> "UNKNOWN"
            }
            
            android.util.Log.d("BluetoothDebug", "Bluetooth not ready (State: $stateText), waiting for it to initialize...")
            
            // Wait for Bluetooth to be ready instead of just failing
            waitForBluetoothReady {
                // This will be called when Bluetooth is ready
                startBluetoothScan()
            }
            return
        }
        
        // Check if already scanning
        if (bluetoothAdapter.isDiscovering) {
            Toast.makeText(this, "Already scanning for devices", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Log Bluetooth state for debugging
        logBluetoothState()
        
        // Clear previous results
        deviceAdapter.clearDevices()
        
        // Start discovery
        if (bluetoothAdapter.startDiscovery()) {
            isScanning = true
            btnScanDevices.text = "Stop Scanning"
            updateStatus("Scanning for devices...", android.R.drawable.ic_search_category_default)
            showProgress(true)
            Toast.makeText(this, "Started scanning for Bluetooth devices", Toast.LENGTH_SHORT).show()
            
            // Set timeout to automatically stop scanning
            scanTimeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            scanTimeoutHandler?.postDelayed({
                if (isScanning) {
                    stopBluetoothScan()
                    Toast.makeText(this@MainActivity, "Scan timeout - stopping automatically", Toast.LENGTH_SHORT).show()
                }
            }, SCAN_TIMEOUT_MS)
        } else {
            // Log detailed error information
            android.util.Log.e("BluetoothDebug", "Failed to start Bluetooth discovery")
            android.util.Log.e("BluetoothDebug", "Adapter enabled: ${bluetoothAdapter.isEnabled}")
            android.util.Log.e("BluetoothDebug", "Adapter state: ${bluetoothAdapter.state}")
            android.util.Log.e("BluetoothDebug", "Already discovering: ${bluetoothAdapter.isDiscovering}")
            
            Toast.makeText(this, "Bluetooth scan failed - Check logs for details", Toast.LENGTH_LONG).show()
            updateStatus("Failed to start scan", android.R.drawable.ic_dialog_alert)
            updatePermissionStatus()
        }
    }
    
    private fun stopBluetoothScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        // Clear timeout handler
        scanTimeoutHandler?.removeCallbacksAndMessages(null)
        scanTimeoutHandler = null
        
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        
        isScanning = false
        showProgress(false)
        val deviceCount = deviceAdapter.getDevices().size
        updateStatus(
            if (deviceCount > 0) "Scan stopped - Found $deviceCount devices" else "Scan stopped - No devices found",
            if (deviceCount > 0) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert
        )
        Toast.makeText(this, "Bluetooth scan stopped", Toast.LENGTH_SHORT).show()
        // Update button text based on current state
        updatePermissionStatus()
    }
    
    private fun updateStatus(status: String, iconRes: Int = android.R.drawable.ic_dialog_info) {
        tvStatus.text = status
        ivStatusIcon.setImageResource(iconRes)
    }
    
    private fun updateDeviceCount() {
        val count = deviceAdapter.getDevices().size
        if (count > 0) {
            tvDeviceCount.text = count.toString()
            tvDeviceCount.visibility = View.VISIBLE
        } else {
            tvDeviceCount.visibility = View.GONE
        }
    }
    
    private fun updatePermissionStatus() {
        if (!checkBluetoothPermissions()) {
            val missingPermissions = getMissingPermissions()
            val statusText = if (missingPermissions.isNotEmpty()) {
                val permanentlyDenied = missingPermissions.any { permission ->
                    when (permission) {
                        "Location" -> !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        "Bluetooth Scan" -> !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_SCAN)
                        "Bluetooth Connect" -> !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BLUETOOTH_CONNECT)
                        else -> false
                    }
                }
                
                if (permanentlyDenied) {
                    "Permissions permanently denied - Go to Settings"
                } else {
                    "Missing: ${missingPermissions.joinToString(", ")}"
                }
            } else {
                "Tap button to grant permissions"
            }
            updateStatus(statusText, android.R.drawable.ic_dialog_alert)
            
            // Update button text to be more informative
//            btnScanDevices.text = if (permanentlyDenied) "Open Settings" else "Grant Permissions"
        } else if (!bluetoothAdapter.isEnabled) {
            updateStatus("Bluetooth is disabled", android.R.drawable.ic_dialog_alert)
            btnScanDevices.text = "Enable Bluetooth"
        } else {
            updateStatus("Ready to scan for Bluetooth devices", android.R.drawable.ic_dialog_info)
            btnScanDevices.text = "Scan Devices"
        }
    }
    
    private fun getMissingPermissions(): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add("Location")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add("Bluetooth Scan")
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add("Bluetooth Connect")
            }
        }
        
        return missingPermissions
    }
    
    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    private fun updateEmptyState() {
        val deviceCount = deviceAdapter.getDevices().size
        if (deviceCount == 0 && !isScanning) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun showProgress(show: Boolean) {
        progressIndicator.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun clearDeviceList() {
        deviceAdapter.clearDevices()
        updateDeviceCount()
        updateEmptyState()
        updateStatus("Device list cleared", android.R.drawable.ic_menu_delete)
        Toast.makeText(this, "Device list cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDeviceDetails(device: BluetoothDeviceItem) {
        val details = buildString {
            appendLine("Device: ${device.getDisplayName()}")
            appendLine("Address: ${device.address}")
            appendLine("Type: ${device.getDeviceTypeString()}")
            appendLine("Bond State: ${device.getBondStateString()}")
            appendLine("Signal: ${device.getSignalStrengthString()}")
            if (device.rssi != 0) {
                appendLine("RSSI: ${device.getRssiDisplayString()}")
            }
            appendLine("Discovered: ${device.getFormattedDiscoveryTime()}")
        }
        
        Toast.makeText(this, details, Toast.LENGTH_LONG).show()
    }
    
    private fun connectToDevice(device: BluetoothDeviceItem) {
        // This is a placeholder for actual connection logic
        // In a real app, you would implement Bluetooth connection here
        Toast.makeText(
            this,
            "Connection to ${device.getDisplayName()} would be implemented here",
            Toast.LENGTH_LONG
        ).show()
    }
    
    override fun onResume() {
        super.onResume()
        // Check if permissions have changed when app resumes
        val previousPermissionsGranted = checkBluetoothPermissions()
        updatePermissionStatus()
        
        // If permissions were previously not granted but are now granted, show a success message
        if (!previousPermissionsGranted && checkBluetoothPermissions()) {
            Toast.makeText(this, "Permissions granted! You can now use Bluetooth features.", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
        }
        
        // Clear timeout handler
        scanTimeoutHandler?.removeCallbacksAndMessages(null)
        scanTimeoutHandler = null
        
        // Clear Bluetooth ready handler
        bluetoothReadyHandler?.removeCallbacksAndMessages(null)
        bluetoothReadyHandler = null
        
        // Stop scanning if still active
        if (isScanning && ::bluetoothAdapter.isInitialized) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.cancelDiscovery()
            }
        }
    }
}
