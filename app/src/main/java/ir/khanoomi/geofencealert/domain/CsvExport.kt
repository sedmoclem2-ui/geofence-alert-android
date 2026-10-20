package ir.khanoomi.geofencealert.domain

import ir.khanoomi.geofencealert.data.EventLog
import java.time.Instant
import java.time.format.DateTimeFormatter

/** ساخت خروجی CSV از تاریخچه رویدادها (منطق خالص، قابل تست). */
object CsvExport {

    /**
     * تبدیل رویدادها به متن CSV با سربرگ «time,zone,type» و زمان ISO-8601.
     * خروجی با BOM یونیکد شروع می‌شود تا در اکسل فارسی درست باز شود.
     * نام محدوده در صورت نیاز (ویرگول/کوتیشن/خط جدید) کوتیشن‌گذاری می‌شود.
     */
    fun eventsToCsv(events: List<EventLog>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("time,zone,type\n")
        for (e in events) {
            sb.append(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(e.timestamp)))
            sb.append(',')
            sb.append(escape(e.zoneName))
            sb.append(',')
            sb.append(e.type)
            sb.append('\n')
        }
        return sb.toString()
    }

    /** کوتیشن‌گذاری استاندارد CSV برای مقادیر مشکل‌دار. */
    private fun escape(value: String): String {
        return if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
