package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.GeoMath
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {

    @Test
    fun `same point gives zero distance`() {
        assertEquals(0.0, GeoMath.distanceMeters(35.6892, 51.3890, 35.6892, 51.3890), 0.001)
    }

    @Test
    fun `one degree of latitude is about 111195 meters`() {
        assertEquals(111195.0, GeoMath.distanceMeters(0.0, 0.0, 1.0, 0.0), 500.0)
    }

    @Test
    fun `tehran to qom is about 128 kilometers`() {
        val d = GeoMath.distanceMeters(35.6892, 51.3890, 34.6394, 50.8759)
        assertEquals(128000.0, d, 4000.0)
    }

    @Test
    fun `small latitude offset of 0_001 degrees is about 111 meters`() {
        assertEquals(111.0, GeoMath.distanceMeters(35.6892, 51.3890, 35.6902, 51.3890), 3.0)
    }
}
