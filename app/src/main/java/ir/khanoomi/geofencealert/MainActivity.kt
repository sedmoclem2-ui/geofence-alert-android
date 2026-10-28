package ir.khanoomi.geofencealert

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import ir.khanoomi.geofencealert.data.BackupManager
import ir.khanoomi.geofencealert.receiver.GpsStateReceiver
import ir.khanoomi.geofencealert.ui.AppNav
import ir.khanoomi.geofencealert.ui.LocalAppLang
import ir.khanoomi.geofencealert.ui.LocalStrings
import ir.khanoomi.geofencealert.ui.TouchRippleHost
import ir.khanoomi.geofencealert.ui.appLangOf
import ir.khanoomi.geofencealert.ui.stringsFor
import ir.khanoomi.geofencealert.ui.stringsForContext
import ir.khanoomi.geofencealert.ui.writeLangMirror
import ir.khanoomi.geofencealert.ui.theme.AppTheme
import ir.khanoomi.geofencealert.ui.theme.CUSTOM_THEME_KEY
import ir.khanoomi.geofencealert.ui.theme.buildCustomPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    companion object {
        /**
         * پیوند ورودیِ در انتظار (موج ۳، اشتراک‌گذاری محدوده).
         * حالت Compose است تا AppNav هم در شروع سرد و هم در تحویل گرم
         * (onNewIntent، اگر launchMode روزی singleTop شود) واکنش نشان
         * دهد؛ AppNav پس از ناوبری آن را null می‌کند تا همان پیوند
         * دوباره باز نشود.
         */
        var pendingImportPayload by mutableStateOf<String?>(null)
    }

    /**
     * شنونده تغییر وضعیت مکان‌یابی — پشتیبانِ شنونده سرویس پایش دقیق؛ تا
     * وقتی برنامه در پیش‌زمینه است، خاموش شدن GPS بی‌درنگ دیده شود، حتی
     * اگر سرویس پایش در حال اجرا نباشد.
     */
    private val gpsStateReceiver = GpsStateReceiver()

    /**
     * استخراج payload از پیوند geofencealert://zone?d=… .
     * نکته launchMode: در مانیفست برای MainActivity هیچ launchModeای
     * تعیین نشده، یعنی حالت پیش‌فرض standard است؛ پس هر ضربه روی پیوند،
     * نمونه تازه‌ای از MainActivity می‌سازد و intent در onCreate می‌رسد
     * (نه onNewIntent). onNewIntent زیر فقط دفاعی است برای روزی که
     * launchMode به singleTop تغییر کند.
     */
    private fun handleZoneLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        if (data.scheme != "geofencealert" || data.host != "zone") return
        val payload = data.getQueryParameter("d")?.takeIf { it.isNotBlank() } ?: return
        pendingImportPayload = payload
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleZoneLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleZoneLink(intent)
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
                    stringsForContext(this@MainActivity).backupRestored,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        setContent {
            val paletteKey by app.prefs.themePalette
                .collectAsStateWithLifecycle(initialValue = "deepblue")
            val customArgb by app.prefs.customPrimaryArgb
                .collectAsStateWithLifecycle(initialValue = 0xFF0E6B5CL)
            val customSecondaryArgb by app.prefs.customSecondaryArgb
                .collectAsStateWithLifecycle(initialValue = 0L)
            val customDark by app.prefs.customDark
                .collectAsStateWithLifecycle(initialValue = false)
            val languageKey by app.prefs.appLanguage
                .collectAsStateWithLifecycle(initialValue = "fa")
            val lang = appLangOf(languageKey)
            // آینه هم‌گام زبان برای لایه‌های غیرکامپوزی (اعلان/ویجت/کاشی/
            // سرویس‌ها)؛ با هر تغییر زبان تازه می‌شود تا کاربرانی که زبان
            // را پیش از وجود آینه انتخاب کرده‌اند هم پوشش داده شوند.
            LaunchedEffect(languageKey) {
                writeLangMirror(this@MainActivity, languageKey)
            }
            val customColors = if (paletteKey == CUSTOM_THEME_KEY) {
                buildCustomPalette(customArgb, customDark, customSecondaryArgb)
            } else {
                null
            }
            // رنگ مکمل دستی کاربر (۰ = خودکار)؛ همان مسیر @ColorInt رنگ اصلی.
            val customSecondary = if (paletteKey == CUSTOM_THEME_KEY && customSecondaryArgb != 0L) {
                Color(customSecondaryArgb.toInt())
            } else {
                null
            }
            AppTheme(
                paletteKey = paletteKey,
                customColors = customColors,
                customSecondary = customSecondary
            ) {
                // زبان برنامه: فارسی و عربی راست‌به‌چپ، انگلیسی چپ‌به‌راست.
                CompositionLocalProvider(
                    LocalStrings provides stringsFor(lang),
                    LocalAppLang provides lang,
                    LocalLayoutDirection provides
                        if (lang == ir.khanoomi.geofencealert.ui.AppLang.EN) {
                            LayoutDirection.Ltr
                        } else {
                            LayoutDirection.Rtl
                        }
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        // موج لمسی سراسری (قطره آب) روی همه صفحه‌ها؛
                        // فقط ناظر است و هیچ رویداد لمسی را مصرف
                        // نمی‌کند، پس هیچ ژستی در برنامه عوض نمی‌شود.
                        TouchRippleHost {
                            AppNav()
                        }
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

    override fun onResume() {
        super.onResume()
        GpsStateReceiver.register(this, gpsStateReceiver)
        // بررسی فوری وضعیت فعلی مکان‌یابی هنگام بازگشت به برنامه
        lifecycleScope.launch { GpsStateReceiver.evaluate(this@MainActivity) }
    }

    override fun onPause() {
        GpsStateReceiver.unregister(this, gpsStateReceiver)
        super.onPause()
    }
}
