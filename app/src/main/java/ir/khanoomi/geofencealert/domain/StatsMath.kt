package ir.khanoomi.geofencealert.domain

import java.time.ZoneId

/**
 * محاسبات خالص آمار ماندگاری و الگوهای ورود (موج ۲، آمار هوشمند).
 *
 * عمداً هیچ وابستگی به اندروید، Room یا Compose ندارد تا مستقیم با
 * JUnit تست شود؛ لایه رابط فقط [EventLog] را به [ZoneEvent] نگاشت می‌کند.
 * مقدارهای نوع رویداد با [ir.khanoomi.geofencealert.data.EventLog] یکسان‌اند.
 */
data class ZoneEvent(
    val zoneId: Long,
    val type: String,
    val timestamp: Long
)

/** خلاصه ماندگاری یک محدوده: فقط جفت‌های کامل ENTER→EXIT شمرده می‌شوند. */
data class StayStats(
    val pairCount: Int,
    val averageMillis: Long,
    val totalMillis: Long
)

object StatsMath {

    const val TYPE_ENTER = "ENTER"
    const val TYPE_EXIT = "EXIT"

    /**
     * جفت‌سازی ماندگاری: برای هر محدوده، هر ENTER با NEXT EXIT بعدی
     * خودش جفت می‌شود. ENTER بی‌جفتِ انتهایی و EXIT سرگردان (بدون ENTER
     * باز) نادیده گرفته می‌شوند؛ ENTER دوم در حالی که یک ENTER باز هست،
     * جفت اول را خراب نمی‌کند (همان ENTER اول با EXIT بعدی جفت می‌شود).
     * محدوده‌ها مستقل از هم پردازش می‌شوند. فقط محدوده‌های دارای حداقل
     * یک جفت کامل در خروجی هستند.
     */
    fun stayDurationsByZone(events: List<ZoneEvent>): Map<Long, List<Long>> {
        val result = mutableMapOf<Long, MutableList<Long>>()
        events.filter { it.type == TYPE_ENTER || it.type == TYPE_EXIT }
            .groupBy { it.zoneId }
            .forEach { (zoneId, zoneEvents) ->
                var pendingEnter: Long? = null
                val durations = mutableListOf<Long>()
                zoneEvents.sortedBy { it.timestamp }.forEach { ev ->
                    when (ev.type) {
                        TYPE_ENTER -> if (pendingEnter == null) pendingEnter = ev.timestamp
                        TYPE_EXIT -> {
                            val enter = pendingEnter
                            if (enter != null) {
                                durations.add((ev.timestamp - enter).coerceAtLeast(0L))
                                pendingEnter = null
                            }
                        }
                    }
                }
                if (durations.isNotEmpty()) result[zoneId] = durations
            }
        return result
    }

    /** میانگین و مجموع ماندگاری هر محدوده، فقط از روی جفت‌های کامل. */
    fun stayStatsByZone(events: List<ZoneEvent>): Map<Long, StayStats> =
        stayDurationsByZone(events).mapValues { (_, durations) ->
            val total = durations.sum()
            StayStats(
                pairCount = durations.size,
                averageMillis = if (durations.isNotEmpty()) total / durations.size else 0L,
                totalMillis = total
            )
        }

    /**
     * سبد نقشه حرارتی ورودها: [day][hour] که day از دوشنبه (۰) تا
     * یکشنبه (۶) با [java.time.DayOfWeek.getValue] منهای یک، و hour ساعت
     * ۰ تا ۲۳ در منطقه زمانی داده‌شده است. فقط ENTERها شمرده می‌شوند؛
     * یک ساعت در دو روز مختلف دو سبد جداست.
     */
    fun entryHeatmap(
        events: List<ZoneEvent>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Array<IntArray> {
        val buckets = Array(7) { IntArray(24) }
        events.filter { it.type == TYPE_ENTER }.forEach { ev ->
            val zoned = java.time.Instant.ofEpochMilli(ev.timestamp).atZone(zone)
            buckets[zoned.dayOfWeek.value - 1][zoned.hour]++
        }
        return buckets
    }

    /** ساعت‌های ورود یک محدوده (۰ تا ۲۳) در منطقه زمانی داده‌شده. */
    fun entryHoursForZone(
        events: List<ZoneEvent>,
        zoneId: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Int> =
        events.filter { it.type == TYPE_ENTER && it.zoneId == zoneId }
            .map { java.time.Instant.ofEpochMilli(it.timestamp).atZone(zone).hour }

    /**
     * پرتکرارترین ساعت ورود؛ تساوی به نفع زودترین ساعت حل می‌شود.
     * با کمتر از ۳ ورود، null برمی‌گردد (یعنی خط الگو نمایش داده نمی‌شود).
     */
    fun mostFrequentEntryHour(hours: List<Int>): Int? {
        if (hours.size < 3) return null
        val counts = hours.groupingBy { it }.eachCount()
        val maxCount = counts.values.maxOrNull() ?: 0
        return counts.filterValues { it == maxCount }.keys.min()
    }
}
