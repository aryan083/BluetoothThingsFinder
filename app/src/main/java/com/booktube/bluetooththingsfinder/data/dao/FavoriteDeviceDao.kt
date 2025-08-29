package com.booktube.bluetooththingsfinder.data.dao

import androidx.room.*
import com.booktube.bluetooththingsfinder.model.FavoriteDevice
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [FavoriteDevice] entity.
 */
@Dao
interface FavoriteDeviceDao {
    /**
     * Get all favorite devices, ordered by when they were added (newest first).
     */
    @Query("SELECT * FROM favorite_devices ORDER BY createdAt DESC")
    fun getAll(): Flow<List<FavoriteDevice>>
    
    /**
     * Get a specific favorite device by its MAC address.
     */
    @Query("SELECT * FROM favorite_devices WHERE macAddress = :macAddress LIMIT 1")
    suspend fun getByMacAddress(macAddress: String): FavoriteDevice?
    
    /**
     * Insert a new favorite device or update if it already exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: FavoriteDevice)
    
    /**
     * Delete a favorite device by its MAC address.
     */
    @Query("DELETE FROM favorite_devices WHERE macAddress = :macAddress")
    suspend fun deleteByMacAddress(macAddress: String)
    
    /**
     * Check if a device with the given MAC address exists in favorites.
     */
    @Query("SELECT EXISTS(SELECT * FROM favorite_devices WHERE macAddress = :macAddress)")
    suspend fun isFavorite(macAddress: String): Boolean
}
