package ir.khanoomi.geofencealert.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * یک رکورد از تاریخچه رویدادهای ژئوفنس.
 *
 * @param zoneId شناسه محدوده‌ای که رویداد برایش رخ داده
 * @param zoneName نام محدوده در لحظه وقوع رویداد (کپی می‌شود تا با تغییر نام، تاریخچه خراب نشود)
 * @param type نوع رویداد: "ENTER" (ورود به محدوده) یا "EXIT" (خروج از محدوده)
 * @param timestamp زمان وقوع به میلی‌ثانیه
 */
@Entity(tableName = "events")
data class EventLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zoneId: Long,
    val zoneName: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_ENTER = "ENTER"
        const val TYPE_EXIT = "EXIT"
    }
}
