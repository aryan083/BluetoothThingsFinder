package com.booktube.bluetooththingsfinder

data class BluetoothDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val deviceType: com.booktube.bluetooththingsfinder.model.DeviceType = com.booktube.bluetooththingsfinder.model.DeviceType.BLE,
    val lastSeen: Long = System.currentTimeMillis(),
    // Major device class from android.bluetooth.BluetoothClass (if available for Classic devices)
    val bluetoothClassMajor: Int? = null,
    // Bond state (e.g., BluetoothDevice.BOND_BONDED = 12, etc.). Optional; used for display only.
    val bondState: Int = 0
)
