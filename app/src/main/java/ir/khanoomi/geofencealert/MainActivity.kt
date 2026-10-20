package ir.khanoomi.geofencealert

import android.os.Bundle
import android.widget.Toast
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
import androidx.lifecycle.lifecycleScope
import ir.khanoomi.geofencealert.data.BackupManager
import ir.khanoomi.geofencealert.ui.AppNav
import ir.khanoomi.geofencealert.ui.LocalStrings
import ir.khanoomi.geofencealert.ui.appLangOf
import ir.khanoomi.geofencealert.ui.stringsFor
import ir.khanoomi.geofencealert.ui.theme.AppTheme
import ir.khanoomi.geofencealert.ui.theme.CUSTOM_THEME_KEY
import ir.khanoomi.geofencealert.ui.theme.buildCustomPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as GeofenceAlertApp

        // بازیابی خودکار از پشتیبان Documents (فقط اگر هیچ محدوده‌ای نداریم؛
        // مثلاً بعد از حذف و نصب دوباره برنامه).
        lifecycleScope.launch {
            val restored = withContext(Dispatchers.IO) {
                BackupManager.tryAutoRestore(this@MainActivity)
            }
            if (restored) {
                Toast.makeText(
                    this@MainActivity,
                    "محدوده‌ها از فایل پشتیبان بازیابی شدند",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        setContent {
            val paletteKey by app.prefs.themePalette
                .collectAsStateWithLifecycle(initialValue = "deepblue")
            val customArgb by app.prefs.customPrimaryArgb
                .collectAsStateWithLifecycle(initialValue = 0xFF0E6B5CL)
            val customDark by app.prefs.customDark
                .collectAsStateWithLifecycle(initialValue = false)
            val languageKey by app.prefs.appLanguage
                .collectAsStateWithLifecycle(initialValue = "fa")
            val lang = appLangOf(languageKey)
            val customColors = if (paletteKey == CUSTOM_THEME_KEY) {
                buildCustomPalette(customArgb, customDark)
            } else {
                null
            }
            AppTheme(paletteKey = paletteKey, customColors = customColors) {
                // زبان برنامه: فارسی و عربی راست‌به‌چپ، انگلیسی چپ‌به‌راست.
                CompositionLocalProvider(
                    LocalStrings provides stringsFor(lang),
                    LocalLayoutDirection provides
                        if (lang == ir.khanoomi.geofencealert.ui.AppLang.EN) {
                            LayoutDirection.Ltr
                        } else {
                            LayoutDirection.Rtl
                        }
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNav()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // تازه‌سازی پشتیبان خودکار Documents در هر شروع برنامه.
        lifecycleScope.launch(Dispatchers.IO) {
            BackupManager.writeAutoBackup(this@MainActivity)
        }
    }
}
