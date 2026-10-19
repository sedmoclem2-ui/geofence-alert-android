package ir.khanoomi.geofencealert.domain

/**
 * اعتبارسنجی خالص ورودی‌های «الگوی تکراری پس از خروج».
 * ارقام فارسی/عربی هم پذیرفته می‌شوند ([ZoneValidator.parseDoubleFa]).
 */
object ExitSequenceValidator {

    /** بازه مجاز تعداد دفعات آلارم. */
    const val MIN_COUNT = 1
    const val MAX_COUNT = 20

    /** بازه مجاز مدت هر آلارم به ثانیه. */
    const val MIN_DURATION_SEC = 3
    const val MAX_DURATION_SEC = 120

    /** بازه مجاز مکث بین آلارم‌ها به ثانیه (صفر یعنی بدون مکث). */
    const val MIN_GAP_SEC = 0
    const val MAX_GAP_SEC = 600

    private fun parseWhole(text: String): Int? {
        val v = ZoneValidator.parseDoubleFa(text) ?: return null
        if (!v.isFinite()) return null
        if (v % 1.0 != 0.0) return null
        return v.toInt()
    }

    /** پیام خطای فارسی تعداد دفعات، یا null در صورت معتبر بودن. */
    fun validateCount(text: String): String? {
        val v = parseWhole(text) ?: return "عدد معتبر وارد کنید."
        return if (v in MIN_COUNT..MAX_COUNT) null
        else "تعداد دفعات باید بین ۱ تا ۲۰ باشد."
    }

    /** پیام خطای فارسی مدت هر آلارم (ثانیه)، یا null در صورت معتبر بودن. */
    fun validateDuration(text: String): String? {
        val v = parseWhole(text) ?: return "عدد معتبر وارد کنید."
        return if (v in MIN_DURATION_SEC..MAX_DURATION_SEC) null
        else "مدت هر آلارم باید بین ۳ تا ۱۲۰ ثانیه باشد."
    }

    /** پیام خطای فارسی مکث بین آلارم‌ها (ثانیه)، یا null در صورت معتبر بودن. */
    fun validateGap(text: String): String? {
        val v = parseWhole(text) ?: return "عدد معتبر وارد کنید."
        return if (v in MIN_GAP_SEC..MAX_GAP_SEC) null
        else "مکث بین آلارم‌ها باید بین ۰ تا ۶۰۰ ثانیه باشد."
    }
}
