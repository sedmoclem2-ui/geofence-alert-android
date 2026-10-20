package ir.khanoomi.geofencealert.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** محاسبات خالص رنگ روی مقادیر ARGB (بدون هیچ وابستگی به اندروید یا Compose). */
object ColorMath {

    /**
     * رنگ مکمل (چرخش ۱۸۰ درجه‌ای رنگ روی چرخ رنگ) برای یک رنگ ARGB؛ آلفا حفظ می‌شود.
     *
     * رنگ‌های تقریباً خاکستری (اشباع نزدیک صفر) مکمل معناداری ندارند و بدون تغییر برمی‌گردند.
     * اگر روشنایی رنگ مکمل خیلی کم باشد، کمی بالا برده می‌شود تا برای لهجه (accent) زنده بماند.
     */
    fun complementArgb(argb: Long): Long {
        val alpha = (argb ushr 24) and 0xFFL
        val r = ((argb ushr 16) and 0xFFL).toInt()
        val g = ((argb ushr 8) and 0xFFL).toInt()
        val b = (argb and 0xFFL).toInt()

        val rf = r / 255.0
        val gf = g / 255.0
        val bf = b / 255.0

        val maxC = max(rf, max(gf, bf))
        val minC = min(rf, min(gf, bf))
        val delta = maxC - minC

        val saturation = if (maxC == 0.0) 0.0 else delta / maxC
        var value = maxC

        // خاکستری‌ها (و سیاه) مکمل ندارند؛ همان رنگ برگردانده می‌شود.
        if (saturation <= 0.04) return argb

        var hue = when {
            delta == 0.0 -> 0.0
            maxC == rf -> 60.0 * (((gf - bf) / delta) % 6.0)
            maxC == gf -> 60.0 * (((bf - rf) / delta) + 2.0)
            else -> 60.0 * (((rf - gf) / delta) + 4.0)
        }
        if (hue < 0.0) hue += 360.0

        val complementHue = (hue + 180.0) % 360.0

        // مکملِ خیلی تیره برای لهجه گم می‌شود؛ روشنایی‌اش را کمی بالا می‌بریم.
        if (value < 0.6) value = 0.6 + value * 0.4

        val (nr, ng, nb) = hsvToRgb(complementHue, saturation, value)
        return (alpha shl 24) or
            (nr.toLong() shl 16) or
            (ng.toLong() shl 8) or
            nb.toLong()
    }

    /** تبدیل HSV (رنگ به درجه، اشباع و روشنایی در بازه ۰ تا ۱) به RGB در بازه ۰ تا ۲۵۵. */
    private fun hsvToRgb(h: Double, s: Double, v: Double): Triple<Int, Int, Int> {
        val c = v * s
        val x = c * (1.0 - abs((h / 60.0) % 2.0 - 1.0))
        val m = v - c
        val (rp, gp, bp) = when {
            h < 60.0 -> Triple(c, x, 0.0)
            h < 120.0 -> Triple(x, c, 0.0)
            h < 180.0 -> Triple(0.0, c, x)
            h < 240.0 -> Triple(0.0, x, c)
            h < 300.0 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        return Triple(
            ((rp + m) * 255.0).roundToInt(),
            ((gp + m) * 255.0).roundToInt(),
            ((bp + m) * 255.0).roundToInt()
        )
    }
}
