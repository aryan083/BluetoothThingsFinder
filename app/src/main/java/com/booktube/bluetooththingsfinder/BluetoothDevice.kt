package com.booktube.bluetooththingsfinder

data class BluetoothDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val deviceType: DeviceType = DeviceType.BLE,
    val lastSeen: Long = System.currentTimeMillis()
)
