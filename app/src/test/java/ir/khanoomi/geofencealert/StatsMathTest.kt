package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.domain.StatsMath
import ir.khanoomi.geofencealert.domain.ZoneEvent
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsMathTest {

    private val min = 60_000L
    private val hour = 3_600_000L

    private fun enter(zone: Long, ts: Long) = ZoneEvent(zone, "ENTER", ts)
    private fun exit(zone: Long, ts: Long) = ZoneEvent(zone, "EXIT", ts)

    /** مهر زمانی UTC برای یک روز/ساعت مشخص (دوشنبه ۲۰۲۶-۱۰-۲۶ به بعد). */
    private fun utcMillis(day: Int, hourOfDay: Int, minute: Int = 0): Long =
        ZonedDateTime.of(2026, 10, day, hourOfDay, minute, 0, 0, ZoneOffset.UTC)
            .toInstant().toEpochMilli()

    // ─── جفت‌سازی ENTER→EXIT ───

    @Test
    fun `normal pair produces its duration`() {
        val durations = StatsMath.stayDurationsByZone(
            listOf(enter(1, 1000), exit(1, 1000 + 60 * min))
        )
        assertEquals(listOf(60 * min), durations[1])
    }

    @Test
    fun `unpaired trailing ENTER is ignored`() {
        val durations = StatsMath.stayDurationsByZone(
            listOf(
                enter(1, 0), exit(1, 30 * min),
                enter(1, 40 * min)
            )
        )
        assertEquals(listOf(30 * min), durations[1])
    }

    @Test
    fun `stray EXIT before any ENTER is ignored`() {
        val durations = StatsMath.stayDurationsByZone(
            listOf(
                exit(1, 0),
                enter(1, 10 * min), exit(1, 50 * min)
            )
        )
        assertEquals(listOf(40 * min), durations[1])
    }

    @Test
    fun `second ENTER while one is pending does not replace it`() {
        val durations = StatsMath.stayDurationsByZone(
            listOf(
                enter(1, 0), enter(1, 10 * min), exit(1, 60 * min)
            )
        )
        assertEquals(listOf(60 * min), durations[1])
    }

    @Test
    fun `interleaved zones pair independently`() {
        val durations = StatsMath.stayDurationsByZone(
            listOf(
                enter(1, 0), enter(2, 5 * min),
                exit(1, 60 * min), exit(2, 25 * min)
            )
        )
        assertEquals(listOf(60 * min), durations[1])
        assertEquals(listOf(20 * min), durations[2])
    }

    @Test
    fun `zone with no complete pair is absent`() {
        val durations = StatsMath.stayDurationsByZone(
            listOf(enter(1, 0), exit(2, 10 * min))
        )
        assertNull(durations[1])
        assertNull(durations[2])
    }

    @Test
    fun `non ENTER EXIT types are ignored`() {
        val durations = StatsMath.stayDurationsByZone(
            listOf(
                ZoneEvent(1, "APPROACH", 0),
                enter(1, 10 * min), exit(1, 40 * min)
            )
        )
        assertEquals(listOf(30 * min), durations[1])
    }

    // ─── میانگین و مجموع ───

    @Test
    fun `average and total from complete pairs only`() {
        val stats = StatsMath.stayStatsByZone(
            listOf(
                enter(1, 0), exit(1, 60 * min),
                enter(1, 100 * min), exit(1, 220 * min),
                enter(1, 300 * min)
            )
        )
        val zone = stats[1]!!
        assertEquals(2, zone.pairCount)
        assertEquals(180 * min, zone.totalMillis)
        assertEquals(90 * min, zone.averageMillis)
    }

    // ─── نقشه حرارتی ───

    @Test
    fun `same hour on different days counted separately`() {
        // ۲۶ اکتبر ۲۰۲۶ دوشنبه و ۲۷ اکتبر سه‌شنبه است.
        val buckets = StatsMath.entryHeatmap(
            listOf(
                enter(1, utcMillis(26, 8)),
                enter(1, utcMillis(27, 8))
            ),
            ZoneOffset.UTC
        )
        assertEquals(1, buckets[0][8])
        assertEquals(1, buckets[1][8])
        assertEquals(0, buckets[2][8])
    }

    @Test
    fun `weekday and hour mapping is exact`() {
        // چهارشنبه ۲۸ اکتبر، ساعت ۱۸.
        val buckets = StatsMath.entryHeatmap(
            listOf(enter(1, utcMillis(28, 18, 45))),
            ZoneOffset.UTC
        )
        assertEquals(1, buckets[2][18])
        assertEquals(0, buckets[2][17])
        assertEquals(0, buckets[2][19])
    }

    @Test
    fun `heatmap counts only ENTER events`() {
        val buckets = StatsMath.entryHeatmap(
            listOf(
                enter(1, utcMillis(26, 9)),
                exit(1, utcMillis(26, 9)),
                ZoneEvent(1, "APPROACH", utcMillis(26, 9))
            ),
            ZoneOffset.UTC
        )
        assertEquals(1, buckets[0][9])
    }

    // ─── پرتکرارترین ساعت ورود ───

    @Test
    fun `mode hour wins`() {
        assertEquals(8, StatsMath.mostFrequentEntryHour(listOf(8, 8, 9)))
    }

    @Test
    fun `tie resolves to earliest hour`() {
        assertEquals(8, StatsMath.mostFrequentEntryHour(listOf(9, 8, 9, 8)))
        assertEquals(7, StatsMath.mostFrequentEntryHour(listOf(7, 8, 9)))
    }

    @Test
    fun `fewer than three entries yields no pattern`() {
        assertNull(StatsMath.mostFrequentEntryHour(listOf(8, 9)))
        assertNull(StatsMath.mostFrequentEntryHour(listOf(8)))
        assertNull(StatsMath.mostFrequentEntryHour(emptyList()))
    }

    @Test
    fun `entry hours for one zone ignore other zones`() {
        val hours = StatsMath.entryHoursForZone(
            listOf(
                enter(1, utcMillis(26, 8)),
                enter(2, utcMillis(26, 20)),
                enter(1, utcMillis(27, 8)),
                enter(1, utcMillis(28, 8)),
                exit(1, utcMillis(28, 9))
            ),
            zoneId = 1,
            zone = ZoneOffset.UTC
        )
        assertEquals(listOf(8, 8, 8), hours)
        assertEquals(8, StatsMath.mostFrequentEntryHour(hours))
    }
}
