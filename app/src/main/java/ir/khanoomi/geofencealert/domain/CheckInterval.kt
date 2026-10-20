package ir.khanoomi.geofencealert.domain

/**
 * فاصله بررسی دقیق (پایش ۲۰ثانیه‌ایِ قابل‌تنظیم) برای محدوده‌های کوچک.
 *
 * کاربر عدد را به ثانیه یا دقیقه وارد می‌کند؛ خروجی همیشه به ثانیه است.
 * مقدار صفر یعنی حالت «مداوم»: بررسی تقریباً پیوسته (هر ۲ ثانیه در سرویس).
 */
object CheckInterval {
    /** کمترین فاصله مجاز بررسی: ۵ ثانیه. */
    const val MIN_SEC = 5

    /** بیشترین فاصله مجاز بررسی: ۶۰ دقیقه. */
    const val MAX_SEC = 3600

    /** حالت مداوم: ۰ یعنی بررسی تقریباً پیوسته. */
    const val CONTINUOUS = 0

    /** آیا مقدار داده‌شده حالت مداوم است. */
    fun isContinuous(seconds: Int): Boolean = seconds == CONTINUOUS

    /**
     * تبدیل ورودی کاربر به ثانیه؛ null یعنی نامعتبر.
     * ارقام فارسی/عربی هم پذیرفته می‌شوند ([ZoneValidator.parseDoubleFa]).
     */
    fun toSeconds(valueText: String, isMinutes: Boolean): Int? {
        val value = ZoneValidator.parseDoubleFa(valueText) ?: return null
        if (!value.isFinite() || value <= 0) return null
        val seconds = (value * if (isMinutes) 60 else 1).toInt()
        return if (seconds in MIN_SEC..MAX_SEC) seconds else null
    }

    /** پیام خطای فارسی، یا null در صورت معتبر بودن (صفر = مداوم، معتبر است). */
    fun errorFa(seconds: Int?): String? = when {
        seconds == null -> "عدد معتبر وارد کنید."
        seconds == CONTINUOUS -> null
        seconds !in MIN_SEC..MAX_SEC -> "بازه مجاز: ۵ ثانیه تا ۶۰ دقیقه."
        else -> null
    }

    /** نمایش فارسی: «مداوم» برای صفر، وگرنه عدد فارسی + «ثانیه». */
    fun formatFa(seconds: Int): String =
        if (isContinuous(seconds)) "مداوم"
        else "${TimeFormat.toFaDigits(seconds.toString())} ثانیه"
}
