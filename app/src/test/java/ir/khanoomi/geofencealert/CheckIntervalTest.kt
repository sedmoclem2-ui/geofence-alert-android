package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.CheckInterval
import org.junit.Assert.*
import org.junit.Test

class CheckIntervalTest {

    @Test
    fun `seconds mode parses plain number`() {
        assertEquals(20, CheckInterval.toSeconds("20", false))
    }

    @Test
    fun `minutes mode multiplies by sixty`() {
        assertEquals(120, CheckInterval.toSeconds("2", true))
    }

    @Test
    fun `persian digits are accepted`() {
        assertEquals(20, CheckInterval.toSeconds("۲۰", false))
    }

    @Test
    fun `zero and negative are invalid`() {
        assertNull(CheckInterval.toSeconds("0", false))
        assertNull(CheckInterval.toSeconds("-5", false))
    }

    @Test
    fun `below minimum is invalid`() {
        assertNull(CheckInterval.toSeconds("4", false))
        assertNotNull(CheckInterval.errorFa(4))
    }

    @Test
    fun `above maximum is invalid`() {
        assertNull(CheckInterval.toSeconds("61", true))
        assertNotNull(CheckInterval.errorFa(3601))
    }

    @Test
    fun `valid value has no error`() {
        assertNull(CheckInterval.errorFa(20))
        assertNull(CheckInterval.errorFa(3600))
    }

    @Test
    fun `null input gives invalid-number error`() {
        assertEquals("عدد معتبر وارد کنید.", CheckInterval.errorFa(null))
    }

    @Test
    fun `out of range gives range error`() {
        assertEquals("بازه مجاز: ۵ ثانیه تا ۶۰ دقیقه.", CheckInterval.errorFa(3))
    }
}
