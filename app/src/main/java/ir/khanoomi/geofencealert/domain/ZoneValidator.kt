package ir.khanoomi.geofencealert.domain

/** نتیجه اعتبارسنجی فرم محدوده؛ خالی بودن خطاها یعنی ورودی معتبر است. */
data class ValidationResult(val errors: List<String>) {
    val isValid: Boolean get() = errors.isEmpty()
}

/** اعتبارسنجی ورودی‌های فرم تعریف محدوده با پیام‌های فارسی. */
object ZoneValidator {

    fun validate(
        name: String,
        latitude: Double?,
        longitude: Double?,
        radius: Double?,
        unit: RadiusUnit
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (name.isBlank()) {
            errors += "نام محدوده را وارد کنید."
        }
        if (latitude == null || !latitude.isFinite() || latitude < -90.0 || latitude > 90.0) {
            errors += "عرض جغرافیایی باید عددی بین -۹۰ و +۹۰ باشد."
        }
        if (longitude == null || !longitude.isFinite() || longitude < -180.0 || longitude > 180.0) {
            errors += "طول جغرافیایی باید عددی بین -۱۸۰ و +۱۸۰ باشد."
        }
        if (radius == null || !radius.isFinite() || radius <= 0.0) {
            errors += "شعاع باید عددی مثبت باشد."
        } else {
            // بررسی معقول بودن شعاع پس از تبدیل به متر (جلوگیری از مقادیر نامعقول)
            val meters = toMeters(radius, unit)
            if (!meters.isFinite() || meters <= 0.0) {
                errors += "شعاع باید عددی مثبت باشد."
            } else if (meters > 1_000_000.0) {
                // B3: سقف ۱۰۰۰ کیلومتر برای شعاع
                errors += "شعاع نمی‌تواند بیشتر از ۱۰۰۰ کیلومتر باشد."
            }
        }
        return ValidationResult(errors)
    }

    /**
     * تبدیل متن ورودی به عدد اعشاری.
     * ارقام فارسی (۰-۹) و عربی (٠-٩) پذیرفته می‌شوند و جداکننده اعشار
     * می‌تواند «.» یا «/» یا «٫» باشد.
     */
    fun parseDoubleFa(raw: String): Double? {
        val s = raw.trim()
        if (s.isEmpty()) return null
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
                in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                '٫', '/' -> sb.append('.')
                // نادیده گرفتن جداکننده هزارگان، فاصله و علائم جهت متن
                '٬', ' ', ' ', '‌', '‎', '‏', '؜' -> { /* ignore */ }
                else -> sb.append(ch)
            }
        }
        return sb.toString().toDoubleOrNull()
    }
}
