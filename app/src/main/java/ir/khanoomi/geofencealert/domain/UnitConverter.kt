package ir.khanoomi.geofencealert.domain

/** واحد شعاع در فرم ورودی. */
enum class RadiusUnit { METER, KILOMETER }

/** تبدیل مقدار شعاع به متر (واحد ذخیره‌سازی و ارسال به Geofencing API). */
fun toMeters(value: Double, unit: RadiusUnit): Double = when (unit) {
    RadiusUnit.METER -> value
    RadiusUnit.KILOMETER -> value * 1000.0
}

private val FA_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

/** تبدیل ارقام لاتین رشته به ارقام فارسی. */
fun toFaDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
        if (ch in '0'..'9') sb.append(FA_DIGITS[ch - '0']) else sb.append(ch)
    }
    return sb.toString()
}

/** نمایش فارسی شعاع؛ مثلاً «۵۰۰ متر» یا «۲ کیلومتر». */
fun formatRadiusFa(meters: Double): String {
    if (!meters.isFinite() || meters < 0) return toFaDigits("0") + " متر"
    return if (meters >= 1000.0 && meters % 1000.0 == 0.0) {
        val km = (meters / 1000.0).toLong().toString()
        "${toFaDigits(km)} کیلومتر"
    } else {
        val m = if (meters % 1.0 == 0.0) meters.toLong().toString() else meters.toString()
        "${toFaDigits(m)} متر"
    }
}
