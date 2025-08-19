package com.booktube.bluetooththingsfinder

object DistanceCalculator {
    
    /**
     * Estimate distance based on RSSI value
     * This is a simplified calculation - real-world accuracy depends on many factors
     */
    fun estimateDistance(rssi: Int): DistanceEstimate {
        // RSSI to distance conversion using a simplified path loss model
        // This is an approximation and may not be accurate in all environments
        
        val distance = when {
            rssi >= -50 -> 0.5f // Very close, within 0.5 meters
            rssi >= -60 -> 1.0f  // Close, within 1 meter
            rssi >= -70 -> 2.0f  // Near, within 2 meters
            rssi >= -80 -> 4.0f  // Medium, within 4 meters
            rssi >= -90 -> 8.0f  // Far, within 8 meters
            rssi >= -100 -> 15.0f // Very far, within 15 meters
            else -> 20.0f // Extremely far, beyond 15 meters
        }
        
        return DistanceEstimate(
            distance = distance,
            confidence = getConfidence(rssi),
            description = getDistanceDescription(distance)
        )
    }
    
    private fun getConfidence(rssi: Int): Float {
        return when {
            rssi >= -50 -> 0.9f // High confidence for very close devices
            rssi >= -70 -> 0.7f  // Good confidence for close devices
            rssi >= -90 -> 0.5f  // Medium confidence for medium distance
            else -> 0.3f          // Low confidence for far devices
        }
    }
    
    private fun getDistanceDescription(distance: Float): String {
        return when {
            distance <= 1.0f -> "Very Close"
            distance <= 2.0f -> "Close"
            distance <= 4.0f -> "Nearby"
            distance <= 8.0f -> "Medium Distance"
            distance <= 15.0f -> "Far"
            else -> "Very Far"
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
            else -> MovementSuggestion.STAY_PUT
        }
    }
}

data class DistanceEstimate(
    val distance: Float,      // Distance in meters
    val confidence: Float,    // Confidence level (0.0 to 1.0)
    val description: String   // Human-readable description
)

enum class MovementSuggestion {
    GETTING_CLOSER,
    GETTING_FARTHER,
    STAY_PUT,
    START_MOVING
}
