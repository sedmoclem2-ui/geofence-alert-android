package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.AlertDecider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertDeciderTest {

    private fun zone(
        isActive: Boolean = true,
        isAlerting: Boolean = false,
        stopMode: String = Zone.STOP_AUTO
    ) = Zone(
        id = 1,
        name = "خانه",
        latitude = 35.7,
        longitude = 51.4,
        radiusMeters = 500.0,
        isActive = isActive,
        stopMode = stopMode,
        isAlerting = isAlerting
    )

    @Test
    fun `enter on active non-alerting zone starts alert`() {
        assertTrue(AlertDecider.shouldStartAlert(zone(), alreadyAlerting = false))
    }

    @Test
    fun `enter on inactive zone does not start alert`() {
        assertFalse(AlertDecider.shouldStartAlert(zone(isActive = false), alreadyAlerting = false))
    }

    @Test
    fun `enter while already alerting does not start duplicate`() {
        assertTrue(AlertDecider.shouldStartAlert(zone(), alreadyAlerting = true).not())
    }

    @Test
    fun `exit stops alert only in auto mode while alerting`() {
        assertTrue(AlertDecider.shouldStopOnExit(zone(stopMode = Zone.STOP_AUTO), isAlerting = true))
    }

    @Test
    fun `exit does not stop alert in manual mode`() {
        assertFalse(
            AlertDecider.shouldStopOnExit(zone(stopMode = Zone.STOP_MANUAL), isAlerting = true)
        )
    }

    @Test
    fun `exit does not stop when not alerting`() {
        assertFalse(AlertDecider.shouldStopOnExit(zone(stopMode = Zone.STOP_AUTO), isAlerting = false))
    }

    @Test
    fun `exit does not stop when not alerting in manual mode`() {
        assertFalse(
            AlertDecider.shouldStopOnExit(zone(stopMode = Zone.STOP_MANUAL), isAlerting = false)
        )
    }
}
