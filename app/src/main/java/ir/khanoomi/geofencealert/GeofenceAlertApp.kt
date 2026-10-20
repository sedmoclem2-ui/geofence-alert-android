package ir.khanoomi.geofencealert

import android.app.Application
import androidx.room.Room
import ir.khanoomi.geofencealert.data.AppDatabase
import ir.khanoomi.geofencealert.data.MIGRATION_1_2
import ir.khanoomi.geofencealert.data.MIGRATION_2_3
import ir.khanoomi.geofencealert.data.MIGRATION_3_4
import ir.khanoomi.geofencealert.data.MIGRATION_4_5
import ir.khanoomi.geofencealert.data.MIGRATION_5_6
import ir.khanoomi.geofencealert.data.MIGRATION_6_7
import ir.khanoomi.geofencealert.data.PrefsRepository
import ir.khanoomi.geofencealert.data.ZoneRepository
import ir.khanoomi.geofencealert.audio.VoiceAnnouncer
import ir.khanoomi.geofencealert.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GeofenceAlertApp : Application() {

    /**
     * کلید زبان ذخیره‌شده («fa»/«en»/«ar») به‌صورت هم‌گام و همیشه تازه؛
     * لایه‌هایی که نمی‌توانند Flow بخوانند (اعلان‌ها، ویجت‌ها، کاشی تنظیمات
     * سریع و سرویس‌ها) از همین‌جا زبان فعلی را می‌گیرند تا هیچ متنی به
     * زبان اشتباه نمایش داده نشود.
     */
    @Volatile
    var currentLanguageKey: String = "fa"
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        // گرم کردن زودهنگام موتور گفتار گوشی تا هنگام ورود به محدوده،
        // اعلام صوتی نام بدون تأخیر راه‌اندازی موتور شروع شود.
        VoiceAnnouncer.init(this)
        // نگهداری نسخه هم‌گامِ زبان انتخاب‌شده برای لایه‌های غیرکامپوزی.
        appScope.launch {
            try {
                prefs.appLanguage.collect { currentLanguageKey = it }
            } catch (_: Exception) {
            }
        }
    }

    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "geofence_alert.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()
    }

    val zoneRepo: ZoneRepository by lazy { ZoneRepository(db.zoneDao()) }

    val prefs: PrefsRepository by lazy { PrefsRepository(this) }
}
