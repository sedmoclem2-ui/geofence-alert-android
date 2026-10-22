package ir.khanoomi.geofencealert.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * رنگ‌های آماده برای محدوده‌ها. اندیس ذخیره‌شده در [ir.khanoomi.geofencealert.data.Zone.colorIndex]
 * از ۱ شروع می‌شود؛ ۰ یعنی «پیش‌فرض تم» و در این نما حل نمی‌شود.
 * یازده رنگ خوش‌خوان و متمایز: قرمز، نارنجی، کهربایی، سبز، فیروزه‌ای،
 * آبی، بنفش، صورتی، مرجانی، ارغوانی و سبزآبی تیره.
 */
object ZoneColors {
    val presets: List<Color> = listOf(
        Color(0xFFEF5350),
        Color(0xFFFB8C00),
        Color(0xFFF9A825),
        Color(0xFF43A047),
        Color(0xFF00ACC1),
        Color(0xFF1E88E5),
        Color(0xFF5E35B1),
        Color(0xFFEC407A),
        Color(0xFFFF7043),
        Color(0xFF8E24AA),
        Color(0xFF00897B)
    )

    /** همان رنگ‌ها به‌صورت ARGB برای جاهایی که عدد خام لازم است. */
    val presetsArgb: List<Int> = presets.map { it.toArgb() }

    /** رنگ متناظر با اندیس؛ برای ۰ یا خارج از بازه null (یعنی پیش‌فرض تم). */
    fun colorOrNull(index: Int): Color? = presets.getOrNull(index - 1)

    /** حل رنگ محدوده: رنگ آماده اگر اندیس معتبر باشد، وگرنه [fallback]. */
    fun zoneColor(index: Int, fallback: Color): Color = colorOrNull(index) ?: fallback
}
