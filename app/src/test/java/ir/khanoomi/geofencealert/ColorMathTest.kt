package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.ColorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست‌های قانون رنگ مکمل (چرخش ۱۸۰ درجه‌ای در چرخ رنگ). */
class ColorMathTest {

    @Test
    fun `red complement is cyan`() {
        assertEquals(0xFF00FFFFL, ColorMath.complementArgb(0xFFFF0000L))
    }

    @Test
    fun `green complement is magenta`() {
        assertEquals(0xFFFF00FFL, ColorMath.complementArgb(0xFF00FF00L))
    }

    @Test
    fun `blue complement is yellow`() {
        assertEquals(0xFFFFFF00L, ColorMath.complementArgb(0xFF0000FFL))
    }

    @Test
    fun `gray has no complement and stays unchanged`() {
        assertEquals(0xFF808080L, ColorMath.complementArgb(0xFF808080L))
    }

    @Test
    fun `alpha channel is preserved`() {
        val result = ColorMath.complementArgb(0x80123456L)
        assertEquals(0x80L, (result ushr 24) and 0xFFL)
    }

    @Test
    fun `navy complement hue is in the orange range`() {
        val result = ColorMath.complementArgb(0xFF0B2A52L)
        val hue = hueOf(result)
        assertTrue("hue=$hue", hue in 20.0..45.0)
    }

    @Test
    fun `double complement returns the original color`() {
        val original = 0xFF1E88E5L
        assertEquals(original, ColorMath.complementArgb(ColorMath.complementArgb(original)))
    }

    /** محاسبه زاویه رنگ (۰ تا ۳۶۰) از یک ARGB، برای استفاده در تست‌ها. */
    private fun hueOf(argb: Long): Double {
        val r = ((argb ushr 16) and 0xFFL).toDouble() / 255.0
        val g = ((argb ushr 8) and 0xFFL).toDouble() / 255.0
        val b = (argb and 0xFFL).toDouble() / 255.0
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val delta = maxC - minC
        if (delta == 0.0) return 0.0
        var hue = when (maxC) {
            r -> 60.0 * (((g - b) / delta) % 6.0)
            g -> 60.0 * (((b - r) / delta) + 2.0)
            else -> 60.0 * (((r - g) / delta) + 4.0)
        }
        if (hue < 0.0) hue += 360.0
        return hue
    }
}
