package ir.khanoomi.geofencealert

import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.ZoneShareCodec
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneShareCodecTest {

    private fun fullZone(): Zone = Zone(
        name = "خانهٔ من",
        latitude = 35.6892,
        longitude = 51.3890,
        radiusMeters = 250.0,
        polygonJson = "[{\"lat\":35.69,\"lng\":51.39},{\"lat\":35.70,\"lng\":51.40}]",
        colorIndex = 5,
        exitBehavior = Zone.EXIT_SEQUENCE,
        alertMode = Zone.ALERT_VIBRATE,
        scheduleEnabled = true,
        scheduleDays = "1,3,5",
        scheduleStartMin = 480,
        scheduleEndMin = 1020,
        silentEnabled = true,
        silentStartMin = 1320,
        silentEndMin = 360,
        soundKey = Zone.SOUND_RINGTONE,
        stopMode = Zone.STOP_MANUAL,
        checkIntervalSec = 45,
        postExitCount = 5,
        postExitDurationSec = 20,
        postExitGapSec = 15,
        approachDistanceM = 300,
        stayReminderMin = 30,
        voiceAnnounce = true
    )

    @Test
    fun `round trip keeps every shareable field`() {
        val zone = fullZone()
        val decoded = ZoneShareCodec.decode(ZoneShareCodec.encode(zone))
        assertTrue(decoded.isSuccess)
        val draft = decoded.getOrThrow()
        assertEquals("خانهٔ من", draft.name)
        assertEquals(35.6892, draft.latitude, 0.0)
        assertEquals(51.3890, draft.longitude, 0.0)
        assertEquals(250.0, draft.radiusMeters, 0.0)
        assertEquals(zone.polygonJson, draft.polygonJson)
        assertEquals(5, draft.colorIndex)
        assertEquals(Zone.EXIT_SEQUENCE, draft.exitBehavior)
        assertEquals(Zone.ALERT_VIBRATE, draft.alertMode)
        assertTrue(draft.scheduleEnabled)
        assertEquals("1,3,5", draft.scheduleDays)
        assertEquals(480, draft.scheduleStartMin)
        assertEquals(1020, draft.scheduleEndMin)
        assertTrue(draft.silentEnabled)
        assertEquals(1320, draft.silentStartMin)
        assertEquals(360, draft.silentEndMin)
        assertEquals(Zone.SOUND_RINGTONE, draft.soundKey)
        assertEquals(Zone.STOP_MANUAL, draft.stopMode)
        assertEquals(45, draft.checkIntervalSec)
        assertEquals(5, draft.postExitCount)
        assertEquals(20, draft.postExitDurationSec)
        assertEquals(15, draft.postExitGapSec)
        assertEquals(300, draft.approachDistanceM)
        assertEquals(30, draft.stayReminderMin)
        assertTrue(draft.voiceAnnounce)
    }

    @Test
    fun `old 15 field payload decodes with zone defaults for new fields`() {
        // پیوندهای ساخته‌شده با ۳٫۹٫۲ و قبل‌تر، ۹ کلید تازه را ندارند؛
        // باید موفق رمزگشایی شوند و فیلدهای تازه مقدار پیش‌فرض Zone را بگیرند.
        val decoded = ZoneShareCodec.decode(
            payloadOf(
                "{\"name\":\"x\",\"lat\":35.0,\"lng\":51.0,\"radiusMeters\":100.0," +
                    "\"polygonJson\":\"\",\"colorIndex\":2,\"exitBehavior\":0," +
                    "\"alertMode\":0,\"scheduleEnabled\":false," +
                    "\"scheduleDays\":\"1,2,3,4,5,6,7\",\"scheduleStartMin\":0," +
                    "\"scheduleEndMin\":1439,\"silentEnabled\":false," +
                    "\"silentStartMin\":1320,\"silentEndMin\":360}"
            )
        )
        assertTrue(decoded.isSuccess)
        val draft = decoded.getOrThrow()
        assertEquals(Zone.SOUND_ALARM, draft.soundKey)
        assertEquals(Zone.STOP_AUTO, draft.stopMode)
        assertEquals(20, draft.checkIntervalSec)
        assertEquals(3, draft.postExitCount)
        assertEquals(10, draft.postExitDurationSec)
        assertEquals(30, draft.postExitGapSec)
        assertEquals(0, draft.approachDistanceM)
        assertEquals(0, draft.stayReminderMin)
        assertFalse(draft.voiceAnnounce)
    }

    @Test
    fun `invalid new field values are rejected`() {
        val base =
            "\"name\":\"x\",\"lat\":35.0,\"lng\":51.0,\"radiusMeters\":100.0"
        // فاصله بررسی صفر یا منفی.
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf("{$base,\"checkIntervalSec\":0}")
            ).isSuccess
        )
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf("{$base,\"checkIntervalSec\":-5}")
            ).isSuccess
        )
        // کلید صدای ناشناخته.
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf("{$base,\"soundKey\":\"megaphone\"}")
            ).isSuccess
        )
        // شیوه توقف ناشناخته.
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf("{$base,\"stopMode\":\"WHATEVER\"}")
            ).isSuccess
        )
        // فاصله نزدیک‌شدن منفی.
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf("{$base,\"approachDistanceM\":-10}")
            ).isSuccess
        )
        // تعداد الگوی خروج منفی و نوع نادرست اعلام صوتی.
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf("{$base,\"postExitCount\":-1}")
            ).isSuccess
        )
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf("{$base,\"voiceAnnounce\":\"yes\"}")
            ).isSuccess
        )
    }

    @Test
    fun `link format is an intent URL targeting the geofencealert scheme`() {
        val link = ZoneShareCodec.linkFor(fullZone())
        assertTrue(link.startsWith("intent://zone?d="))
        assertTrue(
            link.endsWith(
                "#Intent;scheme=geofencealert;package=ir.khanoomi.geofencealert;end"
            )
        )
        val payload = ZoneShareCodec.extractPayload(link)
        assertTrue(payload.matches(Regex("[A-Za-z0-9_-]+")))
        assertTrue(ZoneShareCodec.decode(payload).isSuccess)
    }

    @Test
    fun `extractPayload accepts intent links including trailing text`() {
        val payload = ZoneShareCodec.encode(fullZone())
        val link = ZoneShareCodec.linkFor(fullZone())
        assertEquals(payload, ZoneShareCodec.extractPayload(link))
        assertEquals(payload, ZoneShareCodec.extractPayload("  $link  "))
        assertEquals(payload, ZoneShareCodec.extractPayload("$link some trailing words"))
        assertTrue(ZoneShareCodec.decode(ZoneShareCodec.extractPayload(link)).isSuccess)
    }

    @Test
    fun `extractPayload rejects bare scheme links and raw payloads`() {
        val payload = ZoneShareCodec.encode(fullZone())
        // پیوند قدیمی bare-scheme دیگر پذیرفته نمی‌شود.
        assertEquals("", ZoneShareCodec.extractPayload("geofencealert://zone?d=$payload"))
        assertFalse(
            ZoneShareCodec.decode(
                ZoneShareCodec.extractPayload("geofencealert://zone?d=$payload")
            ).isSuccess
        )
        // payload خام هم قالب پیوند نیست.
        assertEquals("", ZoneShareCodec.extractPayload(payload))
        assertFalse(ZoneShareCodec.decode(ZoneShareCodec.extractPayload(payload)).isSuccess)
    }

    @Test
    fun `extractPayload rejects malformed input`() {
        assertEquals("", ZoneShareCodec.extractPayload("not a link at all"))
        assertFalse(
            ZoneShareCodec.decode(ZoneShareCodec.extractPayload("not a link at all")).isSuccess
        )
        assertEquals("", ZoneShareCodec.extractPayload("intent://zone?d="))
        assertFalse(
            ZoneShareCodec.decode(ZoneShareCodec.extractPayload("intent://zone?d=")).isSuccess
        )
        assertEquals("", ZoneShareCodec.extractPayload(""))
        assertFalse(ZoneShareCodec.decode(ZoneShareCodec.extractPayload("")).isSuccess)
    }

    @Test
    fun `bad base64 is rejected without throwing`() {
        assertFalse(ZoneShareCodec.decode("!!!not-base64!!!").isSuccess)
        assertFalse(ZoneShareCodec.decode("").isSuccess)
    }

    @Test
    fun `bad json is rejected without throwing`() {
        val notJson = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("this is not json".toByteArray(Charsets.UTF_8))
        assertFalse(ZoneShareCodec.decode(notJson).isSuccess)
        val truncated = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"name\":\"x\"".toByteArray(Charsets.UTF_8))
        assertFalse(ZoneShareCodec.decode(truncated).isSuccess)
    }

    private fun payloadOf(json: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.toByteArray(Charsets.UTF_8))

    @Test
    fun `missing name is rejected`() {
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf("{\"lat\":35.0,\"lng\":51.0,\"radiusMeters\":100.0}")
            ).isSuccess
        )
    }

    @Test
    fun `empty name is rejected`() {
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf(
                    "{\"name\":\"   \",\"lat\":35.0,\"lng\":51.0,\"radiusMeters\":100.0}"
                )
            ).isSuccess
        )
    }

    @Test
    fun `out of range latitude is rejected`() {
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf(
                    "{\"name\":\"x\",\"lat\":91.0,\"lng\":51.0,\"radiusMeters\":100.0}"
                )
            ).isSuccess
        )
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf(
                    "{\"name\":\"x\",\"lat\":-90.5,\"lng\":51.0,\"radiusMeters\":100.0}"
                )
            ).isSuccess
        )
    }

    @Test
    fun `out of range longitude is rejected`() {
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf(
                    "{\"name\":\"x\",\"lat\":35.0,\"lng\":181.0,\"radiusMeters\":100.0}"
                )
            ).isSuccess
        )
    }

    @Test
    fun `non positive radius is rejected`() {
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf(
                    "{\"name\":\"x\",\"lat\":35.0,\"lng\":51.0,\"radiusMeters\":0.0}"
                )
            ).isSuccess
        )
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf(
                    "{\"name\":\"x\",\"lat\":35.0,\"lng\":51.0,\"radiusMeters\":-5.0}"
                )
            ).isSuccess
        )
    }

    @Test
    fun `radius above validator maximum is rejected`() {
        assertFalse(
            ZoneShareCodec.decode(
                payloadOf(
                    "{\"name\":\"x\",\"lat\":35.0,\"lng\":51.0,\"radiusMeters\":1000001.0}"
                )
            ).isSuccess
        )
    }

    @Test
    fun `empty polygon round trips as empty`() {
        val zone = fullZone().copy(polygonJson = "")
        val draft = ZoneShareCodec.decode(ZoneShareCodec.encode(zone)).getOrThrow()
        assertEquals("", draft.polygonJson)
    }

    @Test
    fun `unknown extra fields are ignored`() {
        val decoded = ZoneShareCodec.decode(
            payloadOf(
                "{\"name\":\"x\",\"lat\":35.0,\"lng\":51.0,\"radiusMeters\":100.0," +
                    "\"futureField\":{\"a\":[1,2,3]},\"another\":null,\"n\":42}"
            )
        )
        assertTrue(decoded.isSuccess)
        assertEquals("x", decoded.getOrThrow().name)
    }

    @Test
    fun `name with quotes and backslash round trips`() {
        val zone = fullZone().copy(name = "خانه \"من\" \\ تست")
        val draft = ZoneShareCodec.decode(ZoneShareCodec.encode(zone)).getOrThrow()
        assertEquals("خانه \"من\" \\ تست", draft.name)
    }
}
