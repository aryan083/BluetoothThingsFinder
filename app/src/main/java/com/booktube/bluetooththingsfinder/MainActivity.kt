package com.booktube.bluetooththingsfinder

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.booktube.bluetooththingsfinder.ui.BluetoothScannerScreen
import com.booktube.bluetooththingsfinder.ui.theme.BluetoothThingsFinderTheme

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothScanner: BluetoothScanner

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.BLUETOOTH_SCAN] == false -> {
                Toast.makeText(
                    this,
                    "Bluetooth permission is required to scan for nearby devices",
                    Toast.LENGTH_LONG
                ).show()
            }
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == false -> {
                Toast.makeText(
                    this,
                    "Bluetooth permission is required to connect to devices",
                    Toast.LENGTH_LONG
                ).show()
            }
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == false -> {
                Toast.makeText(
                    this,
                    "Location permission is required to scan for nearby devices",
                    Toast.LENGTH_LONG
                ).show()
            }
            permissions[Manifest.permission.BLUETOOTH_ADMIN] == false -> {
                Toast.makeText(
                    this,
                    "Bluetooth admin permission is required to scan for nearby devices",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothScanner = BluetoothScanner(this, bluetoothAdapter)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BluetoothScannerScreen(bluetoothScanner = bluetoothScanner)
                }
            }
        }

        // Request necessary permissions
        requestBluetoothPermissions()
    }

    private fun requestBluetoothPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                bluetoothScanner.startScan()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features will become available or be disabled if the user grants
                // the permission.
                Toast.makeText(
                    this,
                    "Bluetooth permission is required to scan for nearby devices",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN
                    )
                )
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN
                    )
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // Bluetooth was enabled, start scanning
                startBluetoothScan()
            } else {
                // User denied to enable Bluetooth
                Toast.makeText(
                    this,
                    "Bluetooth is required to scan for devices",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startBluetoothScan() {
        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled, request to enable it
            val enableBtIntent = android.content.Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
            return
        }

        // Start scanning
        bluetoothScanner.startScan()
    }

    override fun onDestroy() {
        try {
            bluetoothScanner.onDestroy()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onDestroy: ${e.message}")
        } finally {
            super.onDestroy()
        }
    }
}