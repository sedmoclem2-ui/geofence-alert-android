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
