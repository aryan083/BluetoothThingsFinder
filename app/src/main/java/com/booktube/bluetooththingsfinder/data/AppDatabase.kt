package com.booktube.bluetooththingsfinder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.booktube.bluetooththingsfinder.data.dao.FavoriteDeviceDao
import com.booktube.bluetooththingsfinder.model.FavoriteDevice

/**
 * Room database for the application.
 * 
 * This database stores all the favorite Bluetooth devices that the user has saved.
 * It uses Room's built-in support for LiveData and Flow to provide reactive updates
 * when the data changes.
 */
@Database(
    entities = [FavoriteDevice::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Returns the Data Access Object for favorite devices.
     */
    abstract fun favoriteDeviceDao(): FavoriteDeviceDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Gets the singleton instance of the database.
         * 
         * @param context The application context
         * @return The singleton instance of the database
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bluetooth_finder_db"
                )
                .fallbackToDestructiveMigration() // For development only
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
