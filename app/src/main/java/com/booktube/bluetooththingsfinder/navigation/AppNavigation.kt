package com.booktube.bluetooththingsfinder.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.booktube.bluetooththingsfinder.ui.screens.devicedetail.DeviceDetailScreen
import com.booktube.bluetooththingsfinder.ui.screens.devicelist.DeviceListScreen
import com.booktube.bluetooththingsfinder.ui.screens.navigation.DeviceNavigationScreen

/**
 * Main navigation component that defines all possible routes in the app.
 * 
 * @param navController The NavController that will be used for navigation
 * @param modifier Modifier to be applied to the NavHost
 * @param startDestination The starting destination route (defaults to [NavRoutes.DeviceList])
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NavRoutes.DeviceList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Device List Screen
        composable(NavRoutes.DeviceList.route) {
            DeviceListScreen(
                onDeviceClick = { deviceId ->
                    navController.navigate(NavRoutes.DeviceDetail.createRoute(deviceId))
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.Settings.route)
                }
            )
        }
        
        // Device Detail Screen
        composable(
            route = NavRoutes.DeviceDetail.route,
            arguments = listOf(
                navArgument(NavArgs.DEVICE_ID) {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString(NavArgs.DEVICE_ID) ?: return@composable
            
            DeviceDetailScreen(
                deviceId = deviceId,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToNavigation = { deviceId ->
                    navController.navigate(NavRoutes.Navigation.createRoute(deviceId))
                }
            )
        }
        
        // Navigation Screen
        composable(
            route = NavRoutes.Navigation.route,
            arguments = listOf(
                navArgument(NavArgs.DEVICE_ID) {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString(NavArgs.DEVICE_ID) ?: return@composable
            
            DeviceNavigationScreen(
                deviceId = deviceId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // Settings Screen (to be implemented)
        composable(NavRoutes.Settings.route) {
            // TODO: Implement Settings Screen
            // Temporary back navigation
            navController.navigateUp()
        }
    }
}
