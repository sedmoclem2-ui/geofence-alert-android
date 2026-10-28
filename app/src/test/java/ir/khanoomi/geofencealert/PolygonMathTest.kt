package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.GeoMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolygonMathTest {

    /** مربع واحد (lat,lng): گوشه‌ها (0,0)، (0,10)، (10,10)، (10,0). */
    private val square = listOf(0.0 to 0.0, 0.0 to 10.0, 10.0 to 10.0, 10.0 to 0.0)

    /** چندضلعی مقعر: مربع ۱۰×۱۰ با بریدگی (notch) از بالا تا مرکز. */
    private val concave = listOf(
        0.0 to 0.0,
        0.0 to 10.0,
        10.0 to 10.0,
        10.0 to 6.0,
        5.0 to 6.0,
        5.0 to 4.0,
        10.0 to 4.0,
        10.0 to 0.0
    )

    @Test
    fun `inside point of square is inside`() {
        assertTrue(GeoMath.pointInPolygon(5.0, 5.0, square))
    }

    @Test
    fun `outside point of square is outside`() {
        assertFalse(GeoMath.pointInPolygon(20.0, 5.0, square))
        assertFalse(GeoMath.pointInPolygon(5.0, -1.0, square))
    }

    @Test
    fun `point in concave notch is outside`() {
        // بریدگی: lat در (5,10) و lng در (4,6) بیرون چندضلعی است.
        assertFalse(GeoMath.pointInPolygon(7.0, 5.0, concave))
        // ولی همان lat در خارج از بریدگی داخل است.
        assertTrue(GeoMath.pointInPolygon(7.0, 8.0, concave))
        assertTrue(GeoMath.pointInPolygon(2.0, 5.0, concave))
    }

    @Test
    fun `point exactly on a vertex is inside`() {
        assertTrue(GeoMath.pointInPolygon(0.0, 0.0, square))
        assertTrue(GeoMath.pointInPolygon(10.0, 10.0, square))
    }

    @Test
    fun `point on an edge is inside`() {
        assertTrue(GeoMath.pointInPolygon(0.0, 5.0, square))
        assertTrue(GeoMath.pointInPolygon(5.0, 10.0, square))
    }

    @Test
    fun `degenerate polygon with fewer than 3 points is always outside`() {
        assertFalse(GeoMath.pointInPolygon(5.0, 5.0, emptyList()))
        assertFalse(GeoMath.pointInPolygon(0.0, 0.0, listOf(0.0 to 0.0)))
        assertFalse(GeoMath.pointInPolygon(0.0, 0.0, listOf(0.0 to 0.0, 0.0 to 10.0)))
    }

    @Test
    fun `empty json parses to empty list`() {
        assertTrue(GeoMath.parsePolygonJson("").isEmpty())
        assertTrue(GeoMath.parsePolygonJson("   ").isEmpty())
        assertTrue(GeoMath.parsePolygonJson("[]").isEmpty())
    }

    @Test
    fun `malformed json parses to empty list without throwing`() {
        assertTrue(GeoMath.parsePolygonJson("not json").isEmpty())
        assertTrue(GeoMath.parsePolygonJson("[{lat:1}]").isEmpty())
        assertTrue(GeoMath.parsePolygonJson("""[{"lat":1.0}]""").isEmpty())
        assertTrue(GeoMath.parsePolygonJson("""[{"lng":2.0}]""").isEmpty())
        assertTrue(GeoMath.parsePolygonJson("""[{"lat":"x","lng":2.0}]""").isEmpty())
        assertTrue(GeoMath.parsePolygonJson("[1,2,3]").isEmpty())
    }

    @Test
    fun `valid json parses in order regardless of key order`() {
        val parsed = GeoMath.parsePolygonJson(
            """[{"lng":51.4,"lat":35.7},{"lat":35.8,"lng":51.5}]"""
        )
        assertEquals(listOf(35.7 to 51.4, 35.8 to 51.5), parsed)
    }

    @Test
    fun `centroid is the average of vertices`() {
        val center = GeoMath.centroid(square)
        assertEquals(5.0, center!!.first, 1e-9)
        assertEquals(5.0, center.second, 1e-9)
    }

    @Test
    fun `centroid of empty polygon is null`() {
        assertNull(GeoMath.centroid(emptyList()))
    }
}
