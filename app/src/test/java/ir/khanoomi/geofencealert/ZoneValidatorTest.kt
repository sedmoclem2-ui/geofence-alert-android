package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.RadiusUnit
import ir.khanoomi.geofencealert.domain.ZoneValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneValidatorTest {

    @Test
    fun `valid input passes`() {
        val result = ZoneValidator.validate(
            name = "خانه",
            latitude = 35.7,
            longitude = 51.4,
            radius = 500.0,
            unit = RadiusUnit.METER
        )
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `blank name fails`() {
        val result = ZoneValidator.validate(
            name = "   ",
            latitude = 35.0,
            longitude = 51.0,
            radius = 100.0,
            unit = RadiusUnit.METER
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("نام محدوده") })
    }

    @Test
    fun `latitude boundaries are accepted`() {
        for (lat in listOf(-90.0, 90.0, 0.0)) {
            val result = ZoneValidator.validate("x", lat, 0.0, 10.0, RadiusUnit.METER)
            assertTrue("lat=$lat should be valid", result.isValid)
        }
    }

    @Test
    fun `latitude outside range fails`() {
        for (lat in listOf(-90.1, 90.1, 200.0)) {
            val result = ZoneValidator.validate("x", lat, 0.0, 10.0, RadiusUnit.METER)
            assertFalse("lat=$lat should be invalid", result.isValid)
            assertTrue(result.errors.any { it.contains("عرض جغرافیایی") })
        }
    }

    @Test
    fun `longitude boundaries are accepted`() {
        for (lng in listOf(-180.0, 180.0, 0.0)) {
            val result = ZoneValidator.validate("x", 0.0, lng, 10.0, RadiusUnit.METER)
            assertTrue("lng=$lng should be valid", result.isValid)
        }
    }

    @Test
    fun `longitude outside range fails`() {
        for (lng in listOf(-180.1, 180.1)) {
            val result = ZoneValidator.validate("x", 0.0, lng, 10.0, RadiusUnit.METER)
            assertFalse("lng=$lng should be invalid", result.isValid)
            assertTrue(result.errors.any { it.contains("طول جغرافیایی") })
        }
    }

    @Test
    fun `null coordinates fail`() {
        val result = ZoneValidator.validate("x", null, null, 10.0, RadiusUnit.METER)
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
    }

    @Test
    fun `zero negative and NaN radius fail`() {
        for (r in listOf(0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val result = ZoneValidator.validate("x", 35.0, 51.0, r, RadiusUnit.METER)
            assertFalse("radius=$r should be invalid", result.isValid)
            assertTrue(result.errors.any { it.contains("شعاع") })
        }
    }

    @Test
    fun `null radius fails`() {
        val result = ZoneValidator.validate("x", 35.0, 51.0, null, RadiusUnit.METER)
        assertFalse(result.isValid)
    }

    @Test
    fun `radius above 1000 km fails`() {
        val tooBig = ZoneValidator.validate("x", 35.0, 51.0, 2000.0, RadiusUnit.KILOMETER)
        assertFalse(tooBig.isValid)
        assertTrue(tooBig.errors.any { it.contains("۱۰۰۰ کیلومتر") })

        val tooBigMeters = ZoneValidator.validate("x", 35.0, 51.0, 1_000_001.0, RadiusUnit.METER)
        assertFalse(tooBigMeters.isValid)
    }

    @Test
    fun `radius at 999 km passes`() {
        val ok = ZoneValidator.validate("x", 35.0, 51.0, 999.0, RadiusUnit.KILOMETER)
        assertTrue(ok.isValid)
    }

    // ---- parseDoubleFa ----

    @Test
    fun `parse latin digits with dot`() {
        assertEquals(35.7, ZoneValidator.parseDoubleFa("35.7")!!, 1e-9)
    }

    @Test
    fun `parse persian digits with persian decimal separator`() {
        assertEquals(35.5, ZoneValidator.parseDoubleFa("۳۵٫۵")!!, 1e-9)
    }

    @Test
    fun `parse persian digits with slash separator`() {
        assertEquals(35.5, ZoneValidator.parseDoubleFa("۳۵/۵")!!, 1e-9)
    }

    @Test
    fun `parse arabic digits`() {
        assertEquals(51.4, ZoneValidator.parseDoubleFa("٥١.٤")!!, 1e-9)
    }

    @Test
    fun `parse trims spaces`() {
        assertEquals(12.0, ZoneValidator.parseDoubleFa("  12 ")!!, 1e-9)
    }

    @Test
    fun `parse invalid returns null`() {
        assertNull(ZoneValidator.parseDoubleFa("abc"))
        assertNull(ZoneValidator.parseDoubleFa(""))
        assertNull(ZoneValidator.parseDoubleFa("   "))
    }
}
