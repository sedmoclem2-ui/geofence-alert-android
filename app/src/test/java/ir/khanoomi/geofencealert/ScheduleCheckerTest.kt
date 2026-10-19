package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.ScheduleChecker
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ScheduleCheckerTest {

    /** ساخت لحظه ثابت: 2026-10-17 شنبه است، 2026-10-16 جمعه، 2026-10-18 یکشنبه. */
    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.set(year, month - 1, day, hour, minute, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private val sat0200 = millis(2026, 10, 17, 2, 0)   // شنبه ۰۲:۰۰
    private val sat1000 = millis(2026, 10, 17, 10, 0)  // شنبه ۱۰:۰۰
    private val sat1200 = millis(2026, 10, 17, 12, 0)  // شنبه ۱۲:۰۰
    private val sat2000 = millis(2026, 10, 17, 20, 0)  // شنبه ۲۰:۰۰
    private val fri1000 = millis(2026, 10, 16, 10, 0)  // جمعه ۱۰:۰۰
    private val sun1000 = millis(2026, 10, 18, 10, 0)  // یکشنبه ۱۰:۰۰

    @Test
    fun `dayIndex maps Saturday to 1 and Friday to 7`() {
        assertEquals(1, ScheduleChecker.dayIndex1to7(sat1000))
        assertEquals(7, ScheduleChecker.dayIndex1to7(fri1000))
        assertEquals(2, ScheduleChecker.dayIndex1to7(sun1000))
    }

    @Test
    fun `minutesOfDay extracts hour and minute`() {
        assertEquals(120, ScheduleChecker.minutesOfDay(sat0200))
        assertEquals(720, ScheduleChecker.minutesOfDay(sat1200))
    }

    @Test
    fun `disabled schedule is always in schedule`() {
        assertTrue(ScheduleChecker.isInSchedule(false, "", 0, 0, sat0200))
        assertTrue(ScheduleChecker.isInSchedule(false, "7", 1380, 420, sat1200))
    }

    @Test
    fun `daytime schedule within hours returns true`() {
        assertTrue(ScheduleChecker.isInSchedule(true, "1,2,3,4,5,6,7", 480, 1080, sat1000))
    }

    @Test
    fun `daytime schedule outside hours returns false`() {
        assertFalse(ScheduleChecker.isInSchedule(true, "1,2,3,4,5,6,7", 480, 1080, sat0200))
        assertFalse(ScheduleChecker.isInSchedule(true, "1,2,3,4,5,6,7", 480, 1080, sat2000))
    }

    @Test
    fun `overnight schedule wraps past midnight`() {
        // ۲۳:۰۰ تا ۰۷:۰۰
        assertTrue(ScheduleChecker.isInSchedule(true, "1,2,3,4,5,6,7", 1380, 420, sat0200))
        assertFalse(ScheduleChecker.isInSchedule(true, "1,2,3,4,5,6,7", 1380, 420, sat1200))
    }

    @Test
    fun `day filtering excludes non-listed days`() {
        // فقط جمعه
        assertTrue(ScheduleChecker.isInSchedule(true, "7", 0, 1439, fri1000))
        assertFalse(ScheduleChecker.isInSchedule(true, "7", 0, 1439, sat1000))
    }

    @Test
    fun `bad day tokens are ignored`() {
        assertTrue(ScheduleChecker.isInSchedule(true, "abc,1,,", 0, 1439, sat1000))
        assertFalse(ScheduleChecker.isInSchedule(true, "abc", 0, 1439, sat1000))
    }

    @Test
    fun `quiet disabled is never quiet`() {
        assertFalse(ScheduleChecker.isQuietNow(false, 1380, 420, sat0200))
    }

    @Test
    fun `quiet overnight range works`() {
        assertTrue(ScheduleChecker.isQuietNow(true, 1380, 420, sat0200))
        assertFalse(ScheduleChecker.isQuietNow(true, 1380, 420, sat1200))
    }

    @Test
    fun `quiet daytime range works`() {
        assertTrue(ScheduleChecker.isQuietNow(true, 480, 1080, sat1000))
        assertFalse(ScheduleChecker.isQuietNow(true, 480, 1080, sat0200))
    }
}
