package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.ZoneSort
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoneSortTest {

    private fun zone(id: Long, name: String, radius: Double, sortOrder: Int) = Zone(
        id = id,
        name = name,
        latitude = 35.0,
        longitude = 51.0,
        radiusMeters = radius,
        sortOrder = sortOrder
    )

    @Test
    fun `manual mode orders by sortOrder then id`() {
        val zones = listOf(
            zone(id = 1, name = "الف", radius = 100.0, sortOrder = 3),
            zone(id = 2, name = "ب", radius = 100.0, sortOrder = 1),
            zone(id = 3, name = "ج", radius = 100.0, sortOrder = 2)
        )
        val sorted = ZoneSort.sortZones(zones, ZoneSort.MANUAL)
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun `name mode orders alphabetically`() {
        val zones = listOf(
            zone(id = 1, name = "خانه", radius = 100.0, sortOrder = 0),
            zone(id = 2, name = "اداره", radius = 100.0, sortOrder = 0),
            zone(id = 3, name = "باشگاه", radius = 100.0, sortOrder = 0)
        )
        val sorted = ZoneSort.sortZones(zones, ZoneSort.NAME)
        assertEquals(listOf("اداره", "باشگاه", "خانه"), sorted.map { it.name })
    }

    @Test
    fun `newest mode puts highest id first`() {
        val zones = listOf(
            zone(id = 1, name = "الف", radius = 100.0, sortOrder = 0),
            zone(id = 5, name = "ب", radius = 100.0, sortOrder = 0),
            zone(id = 3, name = "ج", radius = 100.0, sortOrder = 0)
        )
        val sorted = ZoneSort.sortZones(zones, ZoneSort.NEWEST)
        assertEquals(listOf(5L, 3L, 1L), sorted.map { it.id })
    }

    @Test
    fun `radius mode orders by radius ascending`() {
        val zones = listOf(
            zone(id = 1, name = "الف", radius = 500.0, sortOrder = 0),
            zone(id = 2, name = "ب", radius = 100.0, sortOrder = 0),
            zone(id = 3, name = "ج", radius = 200.0, sortOrder = 0)
        )
        val sorted = ZoneSort.sortZones(zones, ZoneSort.RADIUS)
        assertEquals(listOf(2L, 3L, 1L), sorted.map { it.id })
    }
}
