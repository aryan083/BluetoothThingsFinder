package com.booktube.bluetooththingsfinder

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

class DirectionIndicator(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    
    private val _currentDirection = MutableStateFlow(0f)
    val currentDirection: StateFlow<Float> = _currentDirection.asStateFlow()
    
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    
    private var accelerometerSet = false
    private var magnetometerSet = false
    
    fun start() {
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        magnetometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    
    fun stop() {
        sensorManager.unregisterListener(this)
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(sensorEvent.values, 0, accelerometerReading, 0, accelerometerReading.size)
                    accelerometerSet = true
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(sensorEvent.values, 0, magnetometerReading, 0, magnetometerReading.size)
                    magnetometerSet = true
                }
            }
            
            if (accelerometerSet && magnetometerSet) {
                updateDirection()
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    private fun updateDirection() {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthInRadians = orientationAngles[0]
            val azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
            _currentDirection.value = (azimuthInDegrees + 360) % 360
        }
    }
    
    fun getDirectionToDevice(deviceRssi: Int): Direction {
        // Simple direction estimation based on RSSI
        // In a real app, you'd use triangulation with multiple readings
        return when {
            deviceRssi >= -50 -> Direction.VERY_CLOSE
            deviceRssi >= -70 -> Direction.CLOSE
            deviceRssi >= -90 -> Direction.MEDIUM
            else -> Direction.FAR
        }
    }
    
    fun getDirectionArrow(deviceRssi: Int): String {
        return when (getDirectionToDevice(deviceRssi)) {
            Direction.VERY_CLOSE -> "📍" // Very close - you're almost on top of it
            Direction.CLOSE -> "🔍" // Close - look around carefully
            Direction.MEDIUM -> "👀" // Medium distance - scan the area
            Direction.FAR -> "🔭" // Far - you need to move around
        }
    }
}

enum class Direction {
    VERY_CLOSE,
    CLOSE,
    MEDIUM,
    FAR
}
