package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.RadiusUnit
import ir.khanoomi.geofencealert.domain.formatRadiusFa
import ir.khanoomi.geofencealert.domain.toFaDigits
import ir.khanoomi.geofencealert.domain.toMeters
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `meters stay meters`() {
        assertEquals(500.0, toMeters(500.0, RadiusUnit.METER), 1e-9)
    }

    @Test
    fun `kilometers convert to meters`() {
        assertEquals(2500.0, toMeters(2.5, RadiusUnit.KILOMETER), 1e-9)
    }

    @Test
    fun `one kilometer is 1000 meters`() {
        assertEquals(1000.0, toMeters(1.0, RadiusUnit.KILOMETER), 1e-9)
    }

    @Test
    fun `format meters in persian digits`() {
        assertEquals("۵۰۰ متر", formatRadiusFa(500.0))
    }

    @Test
    fun `format whole kilometers`() {
        assertEquals("۲ کیلومتر", formatRadiusFa(2000.0))
    }

    @Test
    fun `format non-whole kilometers stays meters`() {
        assertEquals("۲۵۰۰ متر", formatRadiusFa(2500.0))
    }

    @Test
    fun `toFaDigits converts all digits`() {
        assertEquals("۰۱۲۳۴۵۶۷۸۹", toFaDigits("0123456789"))
    }

    @Test
    fun `toFaDigits keeps other chars`() {
        assertEquals("۵۰۰ متر", toFaDigits("500 متر"))
    }
}
