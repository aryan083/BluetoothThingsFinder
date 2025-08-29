package com.booktube.bluetooththingsfinder

/**
 * Represents a favorite device, storing only essential, non-stateful information.
 * This ensures that favorites are identified by their unique address and display name,
 * without persisting transient data like RSSI or last seen time.
 */
data class FavoriteDevice(
    val name: String,
    val address: String
)
