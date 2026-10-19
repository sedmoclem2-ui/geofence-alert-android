package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    private val now = 1_800_000_000_000L

    @Test
    fun `moments ago for under a minute`() {
        assertEquals("لحظاتی پیش", TimeFormat.formatRelativeTimeFa(now - 30_000, now))
    }

    @Test
    fun `minutes ago in Persian digits`() {
        assertEquals("۵ دقیقه پیش", TimeFormat.formatRelativeTimeFa(now - 5 * 60_000, now))
    }

    @Test
    fun `hours ago in Persian digits`() {
        assertEquals("۳ ساعت پیش", TimeFormat.formatRelativeTimeFa(now - 3 * 3_600_000, now))
    }

    @Test
    fun `yesterday for over a day`() {
        assertEquals("دیروز", TimeFormat.formatRelativeTimeFa(now - 30 * 3_600_000, now))
    }

    @Test
    fun `days ago for several days`() {
        assertEquals("۵ روز پیش", TimeFormat.formatRelativeTimeFa(now - 5 * 86_400_000, now))
    }

    @Test
    fun `invalid timestamp returns empty`() {
        assertEquals("", TimeFormat.formatRelativeTimeFa(0, now))
        assertEquals("", TimeFormat.formatRelativeTimeFa(-10, now))
    }

    @Test
    fun `toFaDigits converts digits`() {
        assertEquals("۱۲۳", TimeFormat.toFaDigits("123"))
        assertEquals("۰", TimeFormat.toFaDigits("0"))
        assertEquals("سال ۱۴۰۴", TimeFormat.toFaDigits("سال 1404"))
    }
}
