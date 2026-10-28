package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.GeoMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * نرمال‌سازی چندضلعی (رفع باگ «ضربدر به‌جای مستطیل»): ترتیب رأس‌ها
 * پس از نرمال‌سازی همیشه چندضلعی‌ای ساده می‌سازد، بی‌آنکه مجموعه
 * نقطه‌ها عوض شود (مگر در مسیر جایگزینِ پوسته محدب).
 */
class PolygonNormalizeTest {

    /** آزمون تقاطع یال‌های ناهمسایه، آینه منطق GeoMath (برای تست). */
    private fun hasCrossing(polygon: List<Pair<Double, Double>>): Boolean {
        val n = polygon.size
        fun orient(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>): Double {
            return (b.second - a.second) * (c.first - a.first) -
                (b.first - a.first) * (c.second - a.second)
        }
        fun onSegment(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>): Boolean {
            return b.second in minOf(a.second, c.second)..maxOf(a.second, c.second) &&
                b.first in minOf(a.first, c.first)..maxOf(a.first, c.first)
        }
        fun crosses(p1: Pair<Double, Double>, p2: Pair<Double, Double>,
                    p3: Pair<Double, Double>, p4: Pair<Double, Double>): Boolean {
            val d1 = orient(p3, p4, p1)
            val d2 = orient(p3, p4, p2)
            val d3 = orient(p1, p2, p3)
            val d4 = orient(p1, p2, p4)
            if (((d1 > 0) != (d2 > 0)) && ((d3 > 0) != (d4 > 0))) return true
            if (d1 == 0.0 && onSegment(p3, p1, p4)) return true
            if (d2 == 0.0 && onSegment(p3, p2, p4)) return true
            if (d3 == 0.0 && onSegment(p1, p3, p2)) return true
            if (d4 == 0.0 && onSegment(p1, p4, p2)) return true
            return false
        }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                if (j == i + 1) continue
                if (i == 0 && j == n - 1) continue
                if (crosses(polygon[i], polygon[(i + 1) % n], polygon[j], polygon[(j + 1) % n])) {
                    return true
                }
            }
        }
        return false
    }

    @Test
    fun `rectangle tapped in diagonal order becomes a simple rectangle`() {
        // کاربر چهار گوشه مستطیل را به‌ترتیب قطری زده است: بالا-چپ،
        // پایین-راست، بالا-راست، پایین-چپ — همان ورودی‌ای که ضربدر می‌ساخت.
        val tapped = listOf(
            35.0 to 51.0, // بالا-چپ
            34.0 to 52.0, // پایین-راست
            35.0 to 52.0, // بالا-راست
            34.0 to 51.0 // پایین-چپ
        )
        val normalized = GeoMath.normalizePolygon(tapped)
        assertEquals(4, normalized.size)
        assertEquals(tapped.toSet(), normalized.toSet())
        assertFalse(hasCrossing(normalized))
    }

    @Test
    fun `explicit bowtie input normalizes to a simple polygon`() {
        val bowtie = listOf(
            0.0 to 0.0,
            1.0 to 1.0,
            0.0 to 1.0,
            1.0 to 0.0
        )
        assertTrue(hasCrossing(bowtie))
        val normalized = GeoMath.normalizePolygon(bowtie)
        assertEquals(4, normalized.size)
        assertEquals(bowtie.toSet(), normalized.toSet())
        assertFalse(hasCrossing(normalized))
    }

    @Test
    fun `concave polygon stays simple and keeps its vertices`() {
        // پیکان مقعر: نقطه فرورفته باید حفظ شود، نه اینکه با پوسته محدب بریده شود.
        val concave = listOf(
            0.0 to 0.0,
            2.0 to 0.0,
            2.0 to 2.0,
            1.0 to 1.0, // فرورفتگی
            0.0 to 2.0
        )
        val normalized = GeoMath.normalizePolygon(concave)
        assertEquals(concave.toSet(), normalized.toSet())
        assertFalse(hasCrossing(normalized))
        // نقطه فرورفتگی باید هنوز روی مرز/داخل باشد، نه حذف‌شده.
        assertTrue(normalized.contains(1.0 to 1.0))
    }

    @Test
    fun `duplicate consecutive points are dropped`() {
        val withDupes = listOf(
            0.0 to 0.0,
            0.0 to 0.0,
            0.0 to 1.0,
            1.0 to 1.0,
            1.0 to 0.0,
            1.0 to 0.0
        )
        val normalized = GeoMath.normalizePolygon(withDupes)
        assertEquals(4, normalized.size)
        assertFalse(hasCrossing(normalized))
    }

    @Test
    fun `fewer than three points returns input unchanged`() {
        val two = listOf(0.0 to 0.0, 1.0 to 1.0)
        assertEquals(two, GeoMath.normalizePolygon(two))
        assertEquals(emptyList<Pair<Double, Double>>(), GeoMath.normalizePolygon(emptyList()))
    }

    @Test
    fun `normalization is idempotent`() {
        val tapped = listOf(
            35.0 to 51.0,
            34.0 to 52.0,
            35.0 to 52.0,
            34.0 to 51.0
        )
        val once = GeoMath.normalizePolygon(tapped)
        assertEquals(once, GeoMath.normalizePolygon(once))
    }

    @Test
    fun `collinear points do not produce a crossing polygon`() {
        val line = listOf(
            0.0 to 0.0,
            0.0 to 1.0,
            0.0 to 2.0,
            0.0 to 3.0
        )
        val normalized = GeoMath.normalizePolygon(line)
        // مسیر پوسته محدب برای هم‌خط‌ها حداکثر دو نقطه می‌دهد؛ مهم این
        // است که خروجی، چندضلعیِ متقاطعِ تباه‌شده نباشد.
        assertTrue(normalized.size <= 2 || !hasCrossing(normalized))
    }

    @Test
    fun `polygon json round trip preserves normalized order`() {
        val tapped = listOf(
            35.0 to 51.0,
            34.0 to 52.0,
            35.0 to 52.0,
            34.0 to 51.0
        )
        val json = GeoMath.polygonToJson(GeoMath.normalizePolygon(tapped))
        val parsed = GeoMath.parsePolygonJson(json)
        assertEquals(4, parsed.size)
        assertFalse(hasCrossing(parsed))
    }
}
