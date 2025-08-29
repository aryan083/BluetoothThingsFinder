package com.booktube.bluetooththingsfinder.model

/**
 * Represents the type of Bluetooth device.
 */
enum class DeviceType {
    /** Bluetooth Low Energy device */
    BLE,
    
    /** Classic Bluetooth device */
    CLASSIC,
    
    /** Dual-mode device (supports both BLE and Classic) */
    DUAL
}
