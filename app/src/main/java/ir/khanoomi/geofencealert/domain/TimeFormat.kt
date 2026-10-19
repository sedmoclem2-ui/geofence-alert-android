package ir.khanoomi.geofencealert.domain

/** قالب‌بندی فارسی زمان‌های نسبی (مثل «۵ دقیقه پیش»). */
object TimeFormat {

    private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    /** تبدیل ارقام انگلیسی رشته به ارقام فارسی. */
    fun toFaDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            sb.append(if (ch in '0'..'9') faDigits[ch - '0'] else ch)
        }
        return sb.toString()
    }

    /**
     * توصیف نسبی فارسی فاصله زمانی تا «الان».
     * اگر [timestamp] نامعتبر (۰ یا منفی) باشد، رشته خالی برمی‌گرداند.
     */
    fun formatRelativeTimeFa(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        if (timestamp <= 0) return ""
        val diff = (now - timestamp).coerceAtLeast(0)
        return when {
            diff < 60_000 -> "لحظاتی پیش"
            diff < 3_600_000 -> "${toFaDigits((diff / 60_000).toString())} دقیقه پیش"
            diff < 86_400_000 -> "${toFaDigits((diff / 3_600_000).toString())} ساعت پیش"
            diff < 172_800_000 -> "دیروز"
            else -> "${toFaDigits((diff / 86_400_000).toString())} روز پیش"
        }
    }
}
