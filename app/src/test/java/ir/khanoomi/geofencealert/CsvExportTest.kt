package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.data.EventLog
import ir.khanoomi.geofencealert.domain.CsvExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست‌های خروجی CSV تاریخچه رویدادها. */
class CsvExportTest {

    @Test
    fun `empty events produces BOM and header only`() {
        val csv = CsvExport.eventsToCsv(emptyList())
        assertEquals("\uFEFFtime,zone,type\n", csv)
    }

    @Test
    fun `row has ISO-8601 time zone name and type`() {
        val events = listOf(
            EventLog(
                id = 1,
                zoneId = 7,
                zoneName = "خانه",
                type = EventLog.TYPE_ENTER,
                timestamp = 0L
            )
        )
        val csv = CsvExport.eventsToCsv(events)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("1970-01-01T00:00:00Z,خانه,ENTER\n"))
    }

    @Test
    fun `zone names with comma or quote are escaped`() {
        val events = listOf(
            EventLog(
                id = 2,
                zoneId = 3,
                zoneName = "دفتر, طبقه \"دوم\"",
                type = EventLog.TYPE_APPROACH,
                timestamp = 1000L
            )
        )
        val csv = CsvExport.eventsToCsv(events)
        assertTrue(csv.contains("\"دفتر, طبقه \"\"دوم\"\"\",APPROACH"))
    }
}
