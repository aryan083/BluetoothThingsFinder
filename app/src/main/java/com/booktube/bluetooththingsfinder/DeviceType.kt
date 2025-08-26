package com.booktube.bluetooththingsfinder

enum class DeviceType(val displayName: String, val shortName: String) {
    BLE("Bluetooth Low Energy", "BLE"),
    CLASSIC("Classic Bluetooth", "BT");
    
    companion object {
        fun fromBluetoothClass(bluetoothClass: Int?): DeviceType {
            // Classic Bluetooth devices have a bluetooth class
            return if (bluetoothClass != null && bluetoothClass != 0) {
                CLASSIC
            } else {
                BLE
            }
        }
    }
}
