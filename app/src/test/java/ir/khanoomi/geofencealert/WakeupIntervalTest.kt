package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.receiver.WakeupTiming
import org.junit.Assert.*
import org.junit.Test

class WakeupIntervalTest {

    @Test
    fun `next trigger is exactly one interval after now`() {
        assertEquals(1_000_000L + 120_000L, WakeupTiming.nextTriggerAtMs(1_000_000L))
    }

    @Test
    fun `interval is two minutes`() {
        assertEquals(120_000L, WakeupTiming.WAKEUP_INTERVAL_MS)
    }

    @Test
    fun `next trigger is always in the future`() {
        val now = System.currentTimeMillis()
        assertTrue(WakeupTiming.nextTriggerAtMs(now) > now)
    }
}
