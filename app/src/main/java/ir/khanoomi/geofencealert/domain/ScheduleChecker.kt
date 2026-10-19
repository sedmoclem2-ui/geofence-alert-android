package ir.khanoomi.geofencealert.domain

import java.util.Calendar

/**
 * بررسی زمان‌بندی محدوده‌ها و ساعات سکوت.
 *
 * قرارداد روز هفته: ۱=شنبه، ۲=یکشنبه، ۳=دوشنبه، ۴=سه‌شنبه،
 * ۵=چهارشنبه، ۶=پنجشنبه، ۷=جمعه.
 */
object ScheduleChecker {

    /** دقیقه از نیمه‌شب برای یک لحظه مشخص. */
    fun minutesOfDay(nowMillis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = nowMillis
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /** شماره روز هفته: ۱=شنبه … ۷=جمعه. */
    fun dayIndex1to7(nowMillis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = nowMillis
        val dow = c.get(Calendar.DAY_OF_WEEK) // SUNDAY=1 … SATURDAY=7
        return (dow % 7) + 1 // SATURDAY(7)→1, SUNDAY(1)→2, …, FRIDAY(6)→7
    }

    private fun inTimeRange(startMin: Int, endMin: Int, m: Int): Boolean =
        if (startMin <= endMin) m in startMin..endMin
        else m >= startMin || m <= endMin // بازه شبانه مثل ۲۳:۰۰ تا ۰۷:۰۰

    /**
     * آیا محدوده در لحظه [nowMillis] طبق زمان‌بندی‌اش فعال است.
     * اگر زمان‌بندی خاموش باشد، همیشه true.
     */
    fun isInSchedule(
        enabled: Boolean,
        daysCsv: String,
        startMin: Int,
        endMin: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!enabled) return true
        val days = daysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
        if (dayIndex1to7(nowMillis) !in days) return false
        return inTimeRange(startMin, endMin, minutesOfDay(nowMillis))
    }

    /** آیا لحظه [nowMillis] داخل بازه ساعات سکوت است. */
    fun isQuietNow(
        enabled: Boolean,
        startMin: Int,
        endMin: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!enabled) return false
        return inTimeRange(startMin, endMin, minutesOfDay(nowMillis))
    }
}
