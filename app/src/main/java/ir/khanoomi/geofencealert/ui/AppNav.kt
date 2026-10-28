package ir.khanoomi.geofencealert.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ir.khanoomi.geofencealert.MainActivity
import kotlinx.coroutines.launch

/** ناوبری اصلی برنامه؛ مقصد شروع به دیده‌شدن آموزش و انتخاب زبان بستگی دارد. */
@Composable
fun AppNav() {
    val context = LocalContext.current
    val app = context.applicationContext as GeofenceAlertApp
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val onboardingDone by app.prefs.onboardingDone
        .collectAsStateWithLifecycle(initialValue = null)
    val languageChosen by app.prefs.languageChosen
        .collectAsStateWithLifecycle(initialValue = null)
    val zones by app.zoneRepo.observeZones()
        .collectAsStateWithLifecycle(initialValue = null)

    // تا خوانده‌شدن تنظیمات، صفحه خالی تا پرش ناخواسته دیده نشود
    if (onboardingDone == null || languageChosen == null || zones == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    // کاربر قدیمی (آموزش را دیده یا محدوده دارد) هرگز به صحنه انتخاب زبان
    // برنمی‌گردد؛ انتخاب زبان فقط برای نصب تازه و پیش از آموزش است.
    val isExistingUser = onboardingDone == true || zones!!.isNotEmpty()
    val needsLanguageScene = languageChosen != true && !isExistingUser

    // ورود از پیوند اشتراک‌گذاری (موج ۳): MainActivity payload را در
    // pendingImportPayload گذاشته است؛ اینجا مصرف و به مسیر import
    // می‌رویم. کدگذاری ناوبری: payload فقط [A-Za-z0-9_-] دارد (Base64
    // نسخه URL-safe بدون padding)، پس ذاتاً در قطعه مسیر امن است، ولی
    // طبق قرارداد مسیرها باز هم با Uri.encode فرستاده و در مقصد با
    // Uri.decode خوانده می‌شود تا اگر روزی الفبا عوض شد نشکند.
    val pendingImport = MainActivity.pendingImportPayload
    LaunchedEffect(pendingImport) {
        if (pendingImport != null) {
            MainActivity.pendingImportPayload = null
            navController.navigate("import/${Uri.encode(pendingImport)}")
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone == true) "home" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                startWithLanguage = needsLanguageScene,
                onLanguageChosen = { key ->
                    scope.launch {
                        app.prefs.setAppLanguage(key)
                        app.prefs.setLanguageChosen(true)
                    }
                },
                onFinish = {
                    scope.launch {
                        app.prefs.setOnboardingDone(true)
                        app.prefs.setLanguageChosen(true)
                    }
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
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
            route = "map_picker?lat={lat}&lng={lng}&radius={radius}&mode={mode}",
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
                    defaultValue = "200.0"
                },
                navArgument("mode") {
                    type = NavType.StringType
                    defaultValue = "circle"
                }
            )
        ) { backStackEntry ->
            MapPickerScreen(
                navController = navController,
                initialLat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0,
                initialLng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0,
                initialRadiusM = backStackEntry.arguments?.getString("radius")?.toDoubleOrNull() ?: 200.0,
                mode = backStackEntry.arguments?.getString("mode") ?: "circle"
            )
        }
        composable("map") {
            LiveMapScreen(navController = navController)
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
        composable(
            route = "import/{payload}",
            arguments = listOf(
                navArgument("payload") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val raw = backStackEntry.arguments?.getString("payload") ?: ""
            ZoneImportScreen(
                navController = navController,
                payload = Uri.decode(raw)
            )
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("battery") {
            BatteryGuideScreen(navController = navController)
        }
    }
}
