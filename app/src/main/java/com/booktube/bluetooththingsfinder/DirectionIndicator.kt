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
import kotlin.math.abs

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
    
    // Smoothing variables for better direction stability
    private val directionHistory = mutableListOf<Float>()
    private val maxHistorySize = 10
    
    // Device tracking for better direction guidance
    private val deviceRssiHistory = mutableMapOf<String, MutableList<Int>>()
    private val maxRssiHistorySize = 20
    
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
        directionHistory.clear()
        deviceRssiHistory.clear()
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
            val normalizedDirection = (azimuthInDegrees + 360) % 360
            
            // Apply smoothing to reduce jitter
            applyDirectionSmoothing(normalizedDirection)
        }
    }
    
    private fun applyDirectionSmoothing(newDirection: Float) {
        directionHistory.add(newDirection)
        if (directionHistory.size > maxHistorySize) {
            directionHistory.removeAt(0)
        }
        
        // Calculate smoothed direction using weighted average
        val smoothedDirection = if (directionHistory.size >= 3) {
            val weights = List(directionHistory.size) { index ->
                (index + 1).toFloat() / directionHistory.size
            }
            val totalWeight = weights.sum()
            val weightedSum = directionHistory.mapIndexed { index, direction ->
                direction * weights[index]
            }.sum()
            weightedSum / totalWeight
        } else {
            newDirection
        }
        
        _currentDirection.value = smoothedDirection
    }
    
    /**
     * Track RSSI changes for a specific device to provide better direction guidance
     */
    fun trackDeviceRssi(deviceAddress: String, rssi: Int) {
        if (!deviceRssiHistory.containsKey(deviceAddress)) {
            deviceRssiHistory[deviceAddress] = mutableListOf()
        }
        
        val history = deviceRssiHistory[deviceAddress]!!
        history.add(rssi)
        
        if (history.size > maxRssiHistorySize) {
            history.removeAt(0)
        }
    }
    
    /**
     * Get movement direction based on RSSI changes for a specific device
     */
    fun getDeviceMovementDirection(deviceAddress: String): MovementDirection {
        val history = deviceRssiHistory[deviceAddress] ?: return MovementDirection.UNKNOWN
        
        if (history.size < 3) return MovementDirection.UNKNOWN
        
        // Calculate RSSI trend
        val recentRssi = history.takeLast(5).average()
        val olderRssi = history.take(5).average()
        val rssiChange = recentRssi - olderRssi
        
        return when {
            rssiChange > 2 -> MovementDirection.GETTING_CLOSER
            rssiChange < -2 -> MovementDirection.GETTING_FARTHER
            abs(rssiChange) <= 2 -> MovementDirection.STABLE
            else -> MovementDirection.UNKNOWN
        }
    }
    
    fun getDirectionToDevice(deviceRssi: Int): Direction {
        // Enhanced direction estimation based on RSSI with more granular levels
        return when {
            deviceRssi >= -45 -> Direction.VERY_CLOSE
            deviceRssi >= -55 -> Direction.EXTREMELY_CLOSE
            deviceRssi >= -65 -> Direction.CLOSE
            deviceRssi >= -75 -> Direction.MEDIUM_CLOSE
            deviceRssi >= -85 -> Direction.MEDIUM
            deviceRssi >= -95 -> Direction.MEDIUM_FAR
            deviceRssi >= -105 -> Direction.FAR
            else -> Direction.VERY_FAR
        }
    }
    
    fun getDirectionArrow(deviceRssi: Int): String {
        return when (getDirectionToDevice(deviceRssi)) {
            Direction.VERY_CLOSE -> "🎯" // Very close - bullseye
            Direction.EXTREMELY_CLOSE -> "📍" // Extremely close - you're almost on top of it
            Direction.CLOSE -> "🔍" // Close - look around carefully
            Direction.MEDIUM_CLOSE -> "👀" // Medium close - scan nearby
            Direction.MEDIUM -> "🔭" // Medium distance - scan the area
            Direction.MEDIUM_FAR -> "🔎" // Medium far - search wider area
            Direction.FAR -> "🏃" // Far - you need to move around
            Direction.VERY_FAR -> "🚶" // Very far - start walking
        }
    }
    
    /**
     * Get detailed guidance for finding a device
     */
    fun getDetailedGuidance(deviceAddress: String, currentRssi: Int): DeviceGuidance {
        val movementDirection = getDeviceMovementDirection(deviceAddress)
        val distanceDirection = getDirectionToDevice(currentRssi)
        
        return DeviceGuidance(
            distanceDirection = distanceDirection,
            movementDirection = movementDirection,
            confidence = calculateConfidence(currentRssi),
            suggestion = generateSuggestion(distanceDirection, movementDirection)
        )
    }
    
    private fun calculateConfidence(rssi: Int): Float {
        return when {
            rssi >= -50 -> 0.95f // Very high confidence for very close devices
            rssi >= -60 -> 0.85f // High confidence for close devices
            rssi >= -70 -> 0.75f // Good confidence for medium-close devices
            rssi >= -80 -> 0.65f // Medium confidence for medium devices
            rssi >= -90 -> 0.55f // Moderate confidence for medium-far devices
            rssi >= -100 -> 0.45f // Low confidence for far devices
            else -> 0.35f // Very low confidence for very far devices
        }
    }
    
    private fun generateSuggestion(distanceDirection: Direction, movementDirection: MovementDirection): String {
        return when {
            distanceDirection == Direction.VERY_CLOSE -> "You're almost on top of it! Look around carefully."
            distanceDirection == Direction.EXTREMELY_CLOSE -> "It's right here! Check your immediate surroundings."
            distanceDirection == Direction.CLOSE && movementDirection == MovementDirection.GETTING_CLOSER -> 
                "Great! You're getting closer. Keep moving in the same direction."
            distanceDirection == Direction.CLOSE && movementDirection == MovementDirection.GETTING_FARTHER -> 
                "You're moving away. Try a different direction."
            distanceDirection == Direction.CLOSE -> "Look around carefully in this area."
            distanceDirection == Direction.MEDIUM_CLOSE && movementDirection == MovementDirection.GETTING_CLOSER -> 
                "Good progress! Continue in this direction."
            distanceDirection == Direction.MEDIUM_CLOSE -> "Scan the nearby area."
            distanceDirection == Direction.MEDIUM && movementDirection == MovementDirection.GETTING_CLOSER -> 
                "You're heading in the right direction. Keep going!"
            distanceDirection == Direction.MEDIUM -> "Walk around and watch for signal changes."
            distanceDirection == Direction.MEDIUM_FAR -> "Move around to get a better signal."
            distanceDirection == Direction.FAR && movementDirection == MovementDirection.GETTING_CLOSER -> 
                "You're making progress! Keep moving in this direction."
            distanceDirection == Direction.FAR -> "Start walking in any direction and monitor signal changes."
            distanceDirection == Direction.VERY_FAR -> "The device is quite far. Start moving in any direction."
            else -> "Move around to find the device."
        }
    }
}

enum class Direction {
    VERY_CLOSE,      // Within 0.3m
    EXTREMELY_CLOSE, // Within 0.5m
    CLOSE,           // Within 1m
    MEDIUM_CLOSE,    // Within 2m
    MEDIUM,          // Within 4m
    MEDIUM_FAR,      // Within 8m
    FAR,             // Within 15m
    VERY_FAR         // Beyond 15m
}

enum class MovementDirection {
    GETTING_CLOSER,
    GETTING_FARTHER,
    STABLE,
    UNKNOWN
}

data class DeviceGuidance(
    val distanceDirection: Direction,
    val movementDirection: MovementDirection,
    val confidence: Float,
    val suggestion: String
)
