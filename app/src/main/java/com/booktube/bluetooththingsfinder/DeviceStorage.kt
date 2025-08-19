package com.booktube.bluetooththingsfinder

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DeviceStorage(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "bluetooth_devices",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val KEY_FAVORITE_DEVICES = "favorite_devices"
        private const val KEY_DEVICE_HISTORY = "device_history"
        private const val MAX_HISTORY_SIZE = 50
    }
    
    fun saveFavoriteDevice(device: BluetoothDevice) {
        val favorites = getFavoriteDevices().toMutableList()
        val existingIndex = favorites.indexOfFirst { it.address == device.address }
        
        if (existingIndex != -1) {
            favorites[existingIndex] = device
        } else {
            favorites.add(device)
        }
        
        saveFavoriteDevices(favorites)
    }
    
    fun removeFavoriteDevice(deviceAddress: String) {
        val favorites = getFavoriteDevices().toMutableList()
        favorites.removeAll { it.address == deviceAddress }
        saveFavoriteDevices(favorites)
    }
    
    fun isFavoriteDevice(deviceAddress: String): Boolean {
        return getFavoriteDevices().any { it.address == deviceAddress }
    }
    
    fun getFavoriteDevices(): List<BluetoothDevice> {
        val json = sharedPreferences.getString(KEY_FAVORITE_DEVICES, "[]")
        val type = object : TypeToken<List<BluetoothDevice>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveFavoriteDevices(devices: List<BluetoothDevice>) {
        val json = gson.toJson(devices)
        sharedPreferences.edit().putString(KEY_FAVORITE_DEVICES, json).apply()
    }
    
    fun addToHistory(device: BluetoothDevice) {
        val history = getDeviceHistory().toMutableList()
        val existingIndex = history.indexOfFirst { it.address == device.address }
        
        if (existingIndex != -1) {
            // Update existing device with new RSSI and timestamp
            history[existingIndex] = device.copy(
                lastSeen = System.currentTimeMillis()
            )
        } else {
            // Add new device
            history.add(device.copy(
                lastSeen = System.currentTimeMillis()
            ))
        }
        
        // Keep only the most recent devices
        if (history.size > MAX_HISTORY_SIZE) {
            history.sortByDescending { it.lastSeen }
            history.removeAt(history.size - 1)
        }
        
        saveDeviceHistory(history)
    }
    
    fun getDeviceHistory(): List<BluetoothDevice> {
        val json = sharedPreferences.getString(KEY_DEVICE_HISTORY, "[]")
        val type = object : TypeToken<List<BluetoothDevice>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveDeviceHistory(devices: List<BluetoothDevice>) {
        val json = gson.toJson(devices)
        sharedPreferences.edit().putString(KEY_DEVICE_HISTORY, json).apply()
    }
    
    fun clearHistory() {
        sharedPreferences.edit().remove(KEY_DEVICE_HISTORY).apply()
    }
    
    fun clearFavorites() {
        sharedPreferences.edit().remove(KEY_FAVORITE_DEVICES).apply()
    }
}
