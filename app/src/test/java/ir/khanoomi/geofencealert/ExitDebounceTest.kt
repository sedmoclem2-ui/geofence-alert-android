package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.service.ExitDebounce
import org.junit.Assert.*
import org.junit.Test

class ExitDebounceTest {

    @Test
    fun `single outside observation does not trigger exit`() {
        val r = ExitDebounce.evaluate(prevInside = true, isInside = false, outsideStreak = 0)
        assertEquals(ExitDebounce.Action.NONE, r.action)
        assertTrue(r.nextInside)
        assertEquals(1, r.nextOutsideStreak)
    }

    @Test
    fun `two consecutive outside observations trigger exit and reset streak`() {
        val r1 = ExitDebounce.evaluate(prevInside = true, isInside = false, outsideStreak = 0)
        assertEquals(ExitDebounce.Action.NONE, r1.action)
        val r2 = ExitDebounce.evaluate(
            prevInside = r1.nextInside,
            isInside = false,
            outsideStreak = r1.nextOutsideStreak
        )
        assertEquals(ExitDebounce.Action.EXIT, r2.action)
        assertFalse(r2.nextInside)
        assertEquals(0, r2.nextOutsideStreak)
    }

    @Test
    fun `inside observation after single outside resets streak without enter or exit`() {
        val r1 = ExitDebounce.evaluate(prevInside = true, isInside = false, outsideStreak = 0)
        assertEquals(1, r1.nextOutsideStreak)
        val r2 = ExitDebounce.evaluate(
            prevInside = r1.nextInside,
            isInside = true,
            outsideStreak = r1.nextOutsideStreak
        )
        assertEquals(ExitDebounce.Action.NONE, r2.action)
        assertTrue(r2.nextInside)
        assertEquals(0, r2.nextOutsideStreak)
    }

    @Test
    fun `outside to inside transition still triggers enter immediately`() {
        val r = ExitDebounce.evaluate(prevInside = false, isInside = true, outsideStreak = 0)
        assertEquals(ExitDebounce.Action.ENTER, r.action)
        assertTrue(r.nextInside)
        assertEquals(0, r.nextOutsideStreak)
    }
}
