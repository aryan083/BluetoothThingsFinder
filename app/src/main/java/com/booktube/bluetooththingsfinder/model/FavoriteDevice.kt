package com.booktube.bluetooththingsfinder.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a device that the user has marked as a favorite.
 * Only stores the essential identifying information (MAC address and name).
 * 
 * @property macAddress The unique MAC address of the device (used as the primary key)
 * @property name The user-assigned or device name
 * @property createdAt Timestamp when the device was favorited
 * @property deviceType The type of the device (BLE, Classic, or Dual)
 */
@Entity(tableName = "favorite_devices")
data class FavoriteDevice(
    @PrimaryKey
    val macAddress: String,
    val name: String,
    val deviceType: DeviceType = DeviceType.BLE,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Creates a FavoriteDevice from a scanned BluetoothDevice
     */
    companion object {
        fun fromBluetoothDevice(device: com.booktube.bluetooththingsfinder.BluetoothDevice): FavoriteDevice {
            return FavoriteDevice(
                macAddress = device.address,
                name = device.name.ifEmpty { "Unknown Device" },
                deviceType = device.deviceType
            )
        }
    }
}
