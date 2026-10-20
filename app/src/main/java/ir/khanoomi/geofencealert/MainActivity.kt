package ir.khanoomi.geofencealert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.khanoomi.geofencealert.ui.AppNav
import ir.khanoomi.geofencealert.ui.theme.AppTheme
import ir.khanoomi.geofencealert.ui.theme.CUSTOM_THEME_KEY
import ir.khanoomi.geofencealert.ui.theme.buildCustomPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as GeofenceAlertApp
        setContent {
            val paletteKey by app.prefs.themePalette
                .collectAsStateWithLifecycle(initialValue = "deepblue")
            val customArgb by app.prefs.customPrimaryArgb
                .collectAsStateWithLifecycle(initialValue = 0xFF0E6B5CL)
            val customDark by app.prefs.customDark
                .collectAsStateWithLifecycle(initialValue = false)
            val customColors = if (paletteKey == CUSTOM_THEME_KEY) {
                buildCustomPalette(customArgb, customDark)
            } else {
                null
            }
            AppTheme(paletteKey = paletteKey, customColors = customColors) {
                // کل رابط کاربری فارسی و راست‌به‌چپ
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNav()
                    }
                }
            }
        }
    }
}
