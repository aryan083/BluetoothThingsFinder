package com.mpc.experinment_8

import android.bluetooth.BluetoothClass
import android.content.Context
import androidx.core.content.ContextCompat

data class BluetoothDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int = 0,
    val deviceClass: BluetoothClass? = null,
    val bondState: Int = 0,
    val discoveredTime: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BluetoothDeviceItem) return false
        return address == other.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
    
    fun getDeviceTypeString(): String {
        return deviceClass?.let {
            when (it.majorDeviceClass) {
                BluetoothClass.Device.Major.PHONE -> "📱 Phone"
                BluetoothClass.Device.Major.COMPUTER -> "💻 Computer"
                BluetoothClass.Device.Major.AUDIO_VIDEO -> "🎧 Audio/Video"
                BluetoothClass.Device.Major.PERIPHERAL -> "⌨️ Peripheral"
                BluetoothClass.Device.Major.IMAGING -> "📷 Imaging"
                BluetoothClass.Device.Major.WEARABLE -> "⌚ Wearable"
                BluetoothClass.Device.Major.TOY -> "🧸 Toy"
                BluetoothClass.Device.Major.HEALTH -> "🏥 Health"
                else -> "📡 Unknown Device"
            }
        } ?: "📡 Unknown Device"
    }
    
    fun getDeviceTypeIcon(): Int {
        return deviceClass?.let {
            when (it.majorDeviceClass) {
                BluetoothClass.Device.Major.PHONE -> android.R.drawable.stat_sys_phone_call
                BluetoothClass.Device.Major.COMPUTER -> android.R.drawable.ic_menu_manage
                BluetoothClass.Device.Major.AUDIO_VIDEO -> android.R.drawable.ic_btn_speak_now
                BluetoothClass.Device.Major.PERIPHERAL -> android.R.drawable.ic_menu_edit
                BluetoothClass.Device.Major.IMAGING -> android.R.drawable.ic_menu_camera
                BluetoothClass.Device.Major.WEARABLE -> android.R.drawable.ic_menu_compass
                BluetoothClass.Device.Major.TOY -> android.R.drawable.ic_menu_rotate
                BluetoothClass.Device.Major.HEALTH -> android.R.drawable.ic_menu_help
                else -> android.R.drawable.stat_sys_data_bluetooth
            }
        } ?: android.R.drawable.stat_sys_data_bluetooth
    }
    
    fun getDeviceTypeColor(context: Context): Int {
        return try {
            deviceClass?.let {
                when (it.majorDeviceClass) {
                    BluetoothClass.Device.Major.PHONE -> ContextCompat.getColor(context, R.color.device_phone)
                    BluetoothClass.Device.Major.COMPUTER -> ContextCompat.getColor(context, R.color.device_computer)
                    BluetoothClass.Device.Major.AUDIO_VIDEO -> ContextCompat.getColor(context, R.color.device_audio)
                    BluetoothClass.Device.Major.PERIPHERAL -> ContextCompat.getColor(context, R.color.device_peripheral)
                    else -> ContextCompat.getColor(context, R.color.device_unknown)
                }
            } ?: ContextCompat.getColor(context, R.color.device_unknown)
        } catch (e: Exception) {
            // Fallback to default colors if custom colors are not available
            android.util.Log.w("BluetoothDeviceItem", "Custom device colors not found, using defaults: ${e.message}")
            deviceClass?.let {
                when (it.majorDeviceClass) {
                    BluetoothClass.Device.Major.PHONE -> ContextCompat.getColor(context, android.R.color.holo_blue_light)
                    BluetoothClass.Device.Major.COMPUTER -> ContextCompat.getColor(context, android.R.color.holo_green_light)
                    BluetoothClass.Device.Major.AUDIO_VIDEO -> ContextCompat.getColor(context, android.R.color.holo_orange_light)
                    BluetoothClass.Device.Major.PERIPHERAL -> ContextCompat.getColor(context, android.R.color.holo_purple)
                    else -> ContextCompat.getColor(context, android.R.color.darker_gray)
                }
            } ?: ContextCompat.getColor(context, android.R.color.darker_gray)
        }
    }
    
    fun getBondStateString(): String {
        return when (bondState) {
            10 -> "Bonded" // BluetoothDevice.BOND_BONDED
            11 -> "Bonding" // BluetoothDevice.BOND_BONDING
            else -> "Not Bonded"
        }
    }
    
    fun getBondStateIcon(): Int {
        return when (bondState) {
            10 -> android.R.drawable.ic_secure // Bonded
            11 -> android.R.drawable.ic_partial_secure // Bonding
            else -> android.R.drawable.ic_lock_lock // Not bonded
        }
    }
    
    fun getSignalStrengthString(): String {
        return when {
            rssi == 0 -> "Unknown"
            rssi >= -50 -> "Strong"
            rssi >= -70 -> "Medium"
            rssi >= -90 -> "Weak"
            else -> "Very Weak"
        }
    }
    
    fun getSignalStrengthColor(context: Context): Int {
        return try {
            when {
                rssi == 0 -> ContextCompat.getColor(context, R.color.signal_very_weak)
                rssi >= -50 -> ContextCompat.getColor(context, R.color.signal_strong)
                rssi >= -70 -> ContextCompat.getColor(context, R.color.signal_medium)
                rssi >= -90 -> ContextCompat.getColor(context, R.color.signal_weak)
                else -> ContextCompat.getColor(context, R.color.signal_very_weak)
            }
        } catch (e: Exception) {
            // Fallback to default colors if custom colors are not available
            android.util.Log.w("BluetoothDeviceItem", "Custom signal colors not found, using defaults: ${e.message}")
            when {
                rssi == 0 -> ContextCompat.getColor(context, android.R.color.darker_gray)
                rssi >= -50 -> ContextCompat.getColor(context, android.R.color.holo_green_light)
                rssi >= -70 -> ContextCompat.getColor(context, android.R.color.holo_orange_light)
                rssi >= -90 -> ContextCompat.getColor(context, android.R.color.holo_red_light)
                else -> ContextCompat.getColor(context, android.R.color.darker_gray)
            }
        }
    }
    
    fun getSignalStrengthIcon(): Int {
        return when {
            rssi == 0 -> android.R.drawable.ic_menu_sort_by_size
            rssi >= -50 -> android.R.drawable.ic_menu_sort_by_size
            rssi >= -70 -> android.R.drawable.ic_menu_sort_by_size
            rssi >= -90 -> android.R.drawable.ic_menu_sort_by_size
            else -> android.R.drawable.ic_menu_sort_by_size
        }
    }
    
    fun getRssiDisplayString(): String {
        return if (rssi != 0) "$rssi dBm" else "Unknown"
    }
    
    fun getFormattedDiscoveryTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - discoveredTime
        
        return when {
            diff < 60_000 -> "Just now" // Less than 1 minute
            diff < 3600_000 -> "${diff / 60_000} min ago" // Less than 1 hour
            diff < 86400_000 -> "${diff / 3600_000} hr ago" // Less than 1 day
            else -> "${diff / 86400_000} day(s) ago" // More than 1 day
        }
    }
    
    fun getDisplayName(): String {
        return name.ifEmpty { "Unknown Device" }
    }
    
    fun isRecent(): Boolean {
        return System.currentTimeMillis() - discoveredTime < 300_000 // Less than 5 minutes
    }
    
    fun getSignalQuality(): Float {
        return when {
            rssi == 0 -> 0.1f
            rssi >= -50 -> 1.0f
            rssi >= -70 -> 0.75f
            rssi >= -90 -> 0.5f
            else -> 0.25f
        }
    }
}
