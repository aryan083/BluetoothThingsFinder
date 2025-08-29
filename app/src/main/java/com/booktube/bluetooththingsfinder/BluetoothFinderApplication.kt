package com.booktube.bluetooththingsfinder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import com.booktube.bluetooththingsfinder.bluetooth.BluetoothScanner
import com.booktube.bluetooththingsfinder.data.AppDatabase
import com.booktube.bluetooththingsfinder.repository.BluetoothRepository
import com.booktube.bluetooththingsfinder.util.Constants.NOTIFICATION_CHANNEL_ID
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.androidx.viewmodel.dsl.viewModel
import timber.log.Timber

/**
 * Application class for the Bluetooth Finder app.
 * 
 * This class is responsible for initializing application-wide components such as:
 * - Dependency injection (Koin)
 * - Database (Room)
 * - Logging (Timber)
 * - Notification channels
 */
class BluetoothFinderApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Create notification channel for foreground service
        createNotificationChannel()
        
        // Initialize Koin for dependency injection
        startKoin {
            androidLogger()
            androidContext(this@BluetoothFinderApplication)
            modules(appModule)
        }
    }
    
    /**
     * Create a notification channel for the foreground service (Android O+).
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Koin module that provides all the dependencies for the application.
     */
    private val appModule = module {
        // Database
        single {
            Room.databaseBuilder(
                androidContext(),
                AppDatabase::class.java, "bluetooth_finder_db"
            ).fallbackToDestructiveMigration()
             .build()
        }
        
        // DAOs
        single { get<AppDatabase>().favoriteDeviceDao() }
        
        // Repository
        single { BluetoothRepository(get()) }
        
        // Bluetooth Scanner
        single { BluetoothScanner(androidContext(), get()) }
        
        // ViewModels
        viewModel { com.booktube.bluetooththingsfinder.ui.screens.devicelist.DeviceListViewModel(get(), get()) }
        
        // Application context
        single<Context> { androidContext() }
    }
    
    companion object {
        init {
            // Enable vector drawable support for API < 21
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
