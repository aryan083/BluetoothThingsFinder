package com.booktube.bluetooththingsfinder

import kotlin.math.pow
import kotlin.math.log10

object DistanceCalculator {
    
    /**
     * Estimate distance based on RSSI value using improved path loss model
     * This uses a more sophisticated calculation that accounts for environmental factors
     */
    fun estimateDistance(rssi: Int): DistanceEstimate {
        // Enhanced RSSI to distance conversion using path loss model
        // Constants for the path loss model
        val measuredPower = -69.0 // Typical measured power at 1 meter
        val n = 2.0 // Path loss exponent (2 for free space, higher for obstacles)
        
        // Calculate distance using path loss model
        val distance = if (rssi == 0) {
            20.0f // Default for unknown RSSI
        } else {
            val ratio = rssi * 1.0 / measuredPower
            val distanceInMeters = 10.0.pow((measuredPower - rssi) / (10 * n))
            distanceInMeters.toFloat()
        }
        
        // Apply reasonable bounds
        val boundedDistance = distance.coerceIn(0.1f, 50.0f)
        
        return DistanceEstimate(
            distance = boundedDistance,
            confidence = getConfidence(rssi, boundedDistance),
            description = getDistanceDescription(boundedDistance),
            accuracy = getAccuracy(boundedDistance)
        )
    }
    
    private fun getConfidence(rssi: Int, distance: Float): Float {
        // Confidence based on RSSI stability and distance
        val rssiConfidence = when {
            rssi >= -50 -> 0.95f // Very high confidence for very close devices
            rssi >= -60 -> 0.85f // High confidence for close devices
            rssi >= -70 -> 0.75f // Good confidence for medium-close devices
            rssi >= -80 -> 0.65f // Medium confidence for medium devices
            rssi >= -90 -> 0.55f // Moderate confidence for medium-far devices
            rssi >= -100 -> 0.45f // Low confidence for far devices
            else -> 0.35f // Very low confidence for very far devices
        }
        
        // Distance-based confidence adjustment
        val distanceConfidence = when {
            distance <= 1.0f -> 0.9f
            distance <= 3.0f -> 0.8f
            distance <= 8.0f -> 0.7f
            distance <= 15.0f -> 0.6f
            else -> 0.5f
        }
        
        // Combine both factors
        return (rssiConfidence * 0.7f + distanceConfidence * 0.3f).coerceIn(0.0f, 1.0f)
    }
    
    private fun getDistanceDescription(distance: Float): String {
        return when {
            distance <= 0.5f -> "Very Close"
            distance <= 1.0f -> "Close"
            distance <= 2.0f -> "Nearby"
            distance <= 4.0f -> "Medium Distance"
            distance <= 8.0f -> "Far"
            distance <= 15.0f -> "Very Far"
            else -> "Extremely Far"
        }
    }
    
    private fun getAccuracy(distance: Float): String {
        return when {
            distance <= 1.0f -> "±0.2m"
            distance <= 3.0f -> "±0.5m"
            distance <= 8.0f -> "±1.0m"
            distance <= 15.0f -> "±2.0m"
            else -> "±5.0m"
        }
    }
    
    /**
     * Get movement suggestions based on current RSSI and previous RSSI
     */
    fun getMovementSuggestion(currentRssi: Int, previousRssi: Int?): MovementSuggestion {
        if (previousRssi == null) {
            return MovementSuggestion.START_MOVING
        }
        
        val rssiDifference = currentRssi - previousRssi
        
        return when {
            rssiDifference > 5 -> MovementSuggestion.GETTING_CLOSER
            rssiDifference < -5 -> MovementSuggestion.GETTING_FARTHER
            abs(rssiDifference.toDouble()) <= 2.0 -> MovementSuggestion.STAY_PUT
            else -> MovementSuggestion.MINOR_CHANGE
        }
    }
    
    /**
     * Calculate signal quality based on RSSI stability
     */
    fun calculateSignalQuality(rssiHistory: List<Int>): SignalQuality {
        if (rssiHistory.size < 3) return SignalQuality.UNKNOWN
        
        val recentRssi = rssiHistory.takeLast(5).average()
        val olderRssi = rssiHistory.take(5).average()
        val variance = rssiHistory.takeLast(10).map { (it.toDouble() - recentRssi).pow(2) }.average()
        
        return when {
            variance < 5.0 && abs(recentRssi - olderRssi) < 3.0 -> SignalQuality.STABLE
            variance < 10.0 && abs(recentRssi - olderRssi) < 5.0 -> SignalQuality.MODERATE
            variance < 20.0 -> SignalQuality.VARIABLE
            else -> SignalQuality.UNSTABLE
        }
    }
    
    /**
     * Get optimal scanning frequency based on distance
     */
    fun getOptimalScanInterval(distance: Float): Long {
        return when {
            distance <= 1.0f -> 1000L // 1 second for very close devices
            distance <= 3.0f -> 2000L // 2 seconds for close devices
            distance <= 8.0f -> 3000L // 3 seconds for medium distance
            distance <= 15.0f -> 5000L // 5 seconds for far devices
            else -> 10000L // 10 seconds for very far devices
        }
    }
    
    private fun abs(value: Double): Double = if (value < 0) -value else value
}

data class DistanceEstimate(
    val distance: Float,      // Distance in meters
    val confidence: Float,    // Confidence level (0.0 to 1.0)
    val description: String,  // Human-readable description
    val accuracy: String      // Accuracy range (e.g., "±0.5m")
)

enum class MovementSuggestion {
    GETTING_CLOSER,
    GETTING_FARTHER,
    STAY_PUT,
    START_MOVING,
    MINOR_CHANGE
}

enum class SignalQuality {
    STABLE,      // Very consistent signal
    MODERATE,    // Some variation but generally stable
    VARIABLE,    // Significant variation
    UNSTABLE,    // Highly variable signal
    UNKNOWN      // Not enough data
}
