package ir.khanoomi.geofencealert.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.khanoomi.geofencealert.GeofenceAlertApp
import kotlinx.coroutines.launch

/** ناوبری اصلی برنامه؛ مقصد شروع به دیده‌شدن آموزش بستگی دارد. */
@Composable
fun AppNav() {
    val context = LocalContext.current
    val app = context.applicationContext as GeofenceAlertApp
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val onboardingDone by app.prefs.onboardingDone
        .collectAsStateWithLifecycle(initialValue = null)

    // تا خوانده‌شدن تنظیمات، صفحه خالی تا پرش ناخواسته دیده نشود
    if (onboardingDone == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone == true) "home" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(onFinish = {
                scope.launch { app.prefs.setOnboardingDone(true) }
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("history") {
            HistoryScreen(navController = navController)
        }
        composable("stats") {
            StatsScreen(navController = navController)
        }
        composable(
            route = "map_picker?lat={lat}&lng={lng}&radius={radius}",
            arguments = listOf(
                navArgument("lat") {
                    type = NavType.StringType
                    defaultValue = "0.0"
                },
                navArgument("lng") {
                    type = NavType.StringType
                    defaultValue = "0.0"
                },
                navArgument("radius") {
                    type = NavType.StringType
                    defaultValue = "500.0"
                }
            )
        ) { backStackEntry ->
            MapPickerScreen(
                navController = navController,
                initialLat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0,
                initialLng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0,
                initialRadiusM = backStackEntry.arguments?.getString("radius")?.toDoubleOrNull() ?: 500.0
            )
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
