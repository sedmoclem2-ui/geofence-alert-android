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
    /** شیوه توقف هشدار: [STOP_AUTO] یا [STOP_MANUAL] */
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
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STOP_AUTO = "AUTO"
        const val STOP_MANUAL = "MANUAL"

        const val SOUND_ALARM = "alarm"
        const val SOUND_RINGTONE = "ringtone"
        const val SOUND_NOTIFICATION = "notification"
    }
}
