package com.booktube.bluetooththingsfinder.navigation

/**
 * Sealed class representing all possible routes in the app.
 * 
 * This helps with type-safe navigation in the app.
 */
sealed class NavRoutes(val route: String) {
    // Main screens
    object DeviceList : NavRoutes("device_list")
    object DeviceDetail : NavRoutes("device_detail/{deviceId}") {
        fun createRoute(deviceId: String) = "device_detail/$deviceId"
    }
    object Navigation : NavRoutes("navigation/{deviceId}") {
        fun createRoute(deviceId: String) = "navigation/$deviceId"
    }
    object Settings : NavRoutes("settings")
    
    // Nested navigation graphs
    object MainGraph : NavRoutes("main") {
        const val ROUTE = "main_route"
    }
    
    // Authentication flow (if needed in the future)
    object AuthGraph : NavRoutes("auth") {
        const val ROUTE = "auth_route"
    }
}

/**
 * Arguments that can be passed between screens.
 */
object NavArgs {
    const val DEVICE_ID = "deviceId"
}
