package ir.khanoomi.geofencealert.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * یک محدوده جغرافیایی تعریف‌شده توسط کاربر.
 * شعاع همیشه به متر ذخیره می‌شود ([radiusMeters]).
 */
@Entity(tableName = "zones")
data class Zone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    /** شعاع محدوده به متر */
    val radiusMeters: Double,
    val isActive: Boolean = true,
    /** شیوه توقف هشدار: [STOP_AUTO] یا [STOP_MANUAL]
     * @deprecated با [exitBehavior] جایگزین شده؛ فقط برای سازگاری داده قدیمی نگه داشته شده است. */
    val stopMode: String = STOP_AUTO,
    /** کلید صدای هشدار: [SOUND_ALARM]، [SOUND_RINGTONE] یا [SOUND_NOTIFICATION] */
    val soundKey: String = SOUND_ALARM,
    /** آیا هم‌اکنون هشدار این محدوده در حال پخش است */
    val isAlerting: Boolean = false,
    /** زمان آخرین ورود ثبت‌شده به این محدوده (میلی‌ثانیه)؛ صفر یعنی هنوز ورود نشده */
    val lastTriggeredAt: Long = 0,
    /** آیا زمان‌بندی برای این محدوده فعال است */
    val scheduleEnabled: Boolean = false,
    /** روزهای فعال زمان‌بندی به‌صورت CSV؛ ۱=شنبه … ۷=جمعه */
    val scheduleDays: String = "1,2,3,4,5,6,7",
    /** شروع بازه زمانی فعال به دقیقه از نیمه‌شب */
    val scheduleStartMin: Int = 0,
    /** پایان بازه زمانی فعال به دقیقه از نیمه‌شب */
    val scheduleEndMin: Int = 1439,
    /** URI صدای دلخواه انتخاب‌شده توسط کاربر؛ null یعنی صدای سیستمی انتخاب‌شده */
    val customSoundUri: String? = null,
    /**
     * فاصله بررسی دقیق به ثانیه، برای محدوده‌های با شعاع کمتر از ۲۰۰ متر.
     * هر چند ثانیه یک بار فاصله واقعی گوشی تا مرکز محدوده حساب شود.
     */
    val checkIntervalSec: Int = 20,
    /**
     * کاربر هشدارِ «این سفر» را دستی متوقف کرده است؛ تا ورود تازه بعدی
     * (سفر جدید) هشدار این محدوده تکرار نمی‌شود.
     */
    val alertAcknowledged: Boolean = false,
    /**
     * رفتار پس از خروج از محدوده: [EXIT_STOP] توقف فوری،
     * [EXIT_SEQUENCE] الگوی تکراری، [EXIT_CONTINUE] ادامه تا توقف دستی.
     */
    val exitBehavior: Int = EXIT_STOP,
    /** تعداد دفعات آلارم در الگوی تکراری پس از خروج */
    val postExitCount: Int = 3,
    /** مدت هر آلارم در الگوی تکراری پس از خروج، به ثانیه */
    val postExitDurationSec: Int = 10,
    /** مکث بین آلارم‌ها در الگوی تکراری پس از خروج، به ثانیه */
    val postExitGapSec: Int = 30,
    /** فاصله هشدار «نزدیک شدن» به متر؛ صفر یعنی خاموش. */
    val approachDistanceM: Int = 0,
    /** یادآور ماندن داخل محدوده به دقیقه؛ صفر یعنی خاموش. */
    val stayReminderMin: Int = 0,
    /**
     * نوع هشدار: [ALERT_BOTH] صدا و لرزش، [ALERT_SOUND] فقط صدا،
     * [ALERT_VIBRATE] فقط لرزش، [ALERT_NONE] هیچ‌کدام (فقط اعلان).
     */
    val alertMode: Int = ALERT_BOTH,
    /** اعلام صوتی نام محدوده هنگام ورود (TTS خود گوشی، بدون هوش مصنوعی). */
    val voiceAnnounce: Boolean = false,
    /** شماره‌های مقصد پیامک اطلاع‌رسانی، جداشده با خط جدید؛ خالی یعنی بدون پیامک. */
    val smsNumbers: String = "",
    /** متن مشترک پیامک اطلاع‌رسانی ورود به محدوده. */
    val smsMessage: String = "",
    /** آیا بازه «اعلان بی‌صدا» برای این محدوده فعال است. */
    val silentEnabled: Boolean = false,
    /** شروع بازه اعلان بی‌صدا به دقیقه از نیمه‌شب (پیش‌فرض ۲۲:۰۰). */
    val silentStartMin: Int = 1320,
    /** پایان بازه اعلان بی‌صدا به دقیقه از نیمه‌شب (پیش‌فرض ۰۶:۰۰). */
    val silentEndMin: Int = 360,
    /** زمان ورود فعلی به محدوده (میلی‌ثانیه)؛ صفر یعنی هم‌اکنون داخل نیستیم. */
    val insideSince: Long = 0,
    /** آیا یادآور ماندنِ این اقامت قبلاً ارسال شده است. */
    val stayReminded: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STOP_AUTO = "AUTO"
        const val STOP_MANUAL = "MANUAL"

        /** رفتارهای پس از خروج از محدوده (جایگزین [stopMode]). */
        const val EXIT_STOP = 0
        const val EXIT_SEQUENCE = 1
        const val EXIT_CONTINUE = 2

        /** انواع هشدار هنگام ورود به محدوده. */
        const val ALERT_BOTH = 0
        const val ALERT_SOUND = 1
        const val ALERT_VIBRATE = 2
        const val ALERT_NONE = 3

        const val SOUND_ALARM = "alarm"
        const val SOUND_RINGTONE = "ringtone"
        const val SOUND_NOTIFICATION = "notification"
    }
}
