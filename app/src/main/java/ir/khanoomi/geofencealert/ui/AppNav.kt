package ir.khanoomi.geofencealert.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/** ناوبری اصلی برنامه. */
@Composable
fun AppNav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "edit?zoneId={zoneId}",
            arguments = listOf(
                navArgument("zoneId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val zoneId = backStackEntry.arguments?.getLong("zoneId") ?: -1L
            ZoneEditScreen(navController = navController, zoneId = zoneId)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("battery") {
            BatteryGuideScreen(navController = navController)
        }
    }
}
