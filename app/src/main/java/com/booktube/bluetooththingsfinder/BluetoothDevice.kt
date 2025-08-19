package com.booktube.bluetooththingsfinder

data class BluetoothDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis()
)
