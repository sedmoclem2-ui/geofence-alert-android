package ir.khanoomi.geofencealert.domain

import ir.khanoomi.geofencealert.data.Zone
import java.util.Base64
import java.util.Locale

/**
 * پیش‌نویس محدوده‌ای که از یک پیوند اشتراک‌گذاری بازیابی شده است.
 * فقط فیلدهای قابل‌اشتراک را دارد؛ بقیه فیلدهای [Zone] هنگام افزودن
 * از مقدارهای پیش‌فرض خودشان ساخته می‌شوند. شناسه ندارد چون محدوده
 * واردشده همیشه تازه است و با isActive=false درج می‌شود (محل درج،
 * نه این کدک، آن را تعیین می‌کند).
 */
data class ZoneDraft(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val polygonJson: String = "",
    val colorIndex: Int = 0,
    val exitBehavior: Int = Zone.EXIT_STOP,
    val alertMode: Int = Zone.ALERT_BOTH,
    val scheduleEnabled: Boolean = false,
    val scheduleDays: String = "1,2,3,4,5,6,7",
    val scheduleStartMin: Int = 0,
    val scheduleEndMin: Int = 1439,
    val silentEnabled: Boolean = false,
    val silentStartMin: Int = 1320,
    val silentEndMin: Int = 360
)

/**
 * کدک پیوند اشتراک‌گذاری محدوده (موج ۳).
 *
 * قالب پیوند (از ۳٫۹٫۲): `intent://zone?d=<payload>#Intent;scheme=geofencealert;package=ir.khanoomi.geofencealert;end`
 * که payload همان خروجی [encode] است: JSON فشرده از فقط ۱۵ فیلد قابل‌اشتراک، کدشده با
 * Base64 نسخه URL-safe بدون padding (java.util، نه android.util، تا
 * همین کد مستقیم در تست‌های JVM هم اجرا شود؛ minSdk برابر ۲۶ است، پس
 * روی دستگاه هم java.util.Base64 در دسترس است).
 *
 * JSON دستی ساخته و پارس می‌شود (بدون org.json که در تست JVM نیست):
 * رشته‌ها با escape کامل (کوتیشن، بک‌اسلش و کاراکترهای کنترلی) نوشته
 * می‌شوند و پارسِر کلیدهای ناشناخته را نادیده می‌گیرد تا پیوندهای
 * نسخه‌های آینده هم خوانده شوند.
 *
 * [decode] هرگز throw نمی‌کند: Base64 خراب، JSON خراب، نام خالی،
 * مختصات خارج از بازه یا شعاع نامعتبر همگی Result.failure برمی‌گردانند.
 * سقف شعاع همان سقف [ZoneValidator] است: ۱٬۰۰۰٬۰۰۰ متر.
 */
object ZoneShareCodec {

    const val SCHEME = "geofencealert"
    const val HOST = "zone"
    const val PARAM = "d"
    const val PACKAGE = "ir.khanoomi.geofencealert"
    const val MAX_RADIUS_METERS = 1_000_000.0

    /**
     * ساخت پیوند کامل از یک محدوده — فقط در قالب intent:// تا مرورگرها
     * آن را به‌عنوان intent بفهمند و به intent-filter همان scheme بسپارند
     * (manifest تغییری نمی‌خواهد؛ scheme و package داخل intent همان
     * فیلتر موجود را هدف می‌گیرند). هیچ پیوند bare-scheme دیگری هرگز
     * فرستاده نمی‌شود و در دیالوگ واردات هم پذیرفته نمی‌شود.
     */
    fun linkFor(zone: Zone): String =
        "intent://$HOST?$PARAM=${encode(zone)}#Intent;scheme=$SCHEME;package=$PACKAGE;end"

    /**
     * استخراج payload فقط از پیوندهای قالب تازه (intent://): مقدار `d=`
     * تا اولین `#` یا `&` یا فاصله. هر ورودی دیگری — از جمله پیوندهای
     * قدیمیِ bare-scheme (`geofencealert://zone?d=…`) و payload خام —
     * نامعتبر است و رشته خالی برمی‌گردد ([decode] آن را رد می‌کند).
     * الفبای payload (Base64 امن برای URL بدون padding) نه `=` دارد نه
     * `#` و نه `&`، پس برش‌ها امن‌اند.
     */
    fun extractPayload(input: String): String {
        val trimmed = input.trim()
        if (!trimmed.startsWith("intent://")) return ""
        val dIndex = trimmed.indexOf("$PARAM=")
        if (dIndex < 0) return ""
        val after = trimmed.substring(dIndex + PARAM.length + 1)
        val end = after.indexOfFirst { it == '#' || it == '&' || it.isWhitespace() }
        return if (end < 0) after else after.substring(0, end)
    }

    /** کدگذاری یک محدوده به payload قابل‌جایگذاری در پیوند. */
    fun encode(zone: Zone): String {
        val json = buildString {
            append('{')
            append("\"name\":").append(jsonString(zone.name))
            append(",\"lat\":").append(num(zone.latitude))
            append(",\"lng\":").append(num(zone.longitude))
            append(",\"radiusMeters\":").append(num(zone.radiusMeters))
            append(",\"polygonJson\":").append(jsonString(zone.polygonJson))
            append(",\"colorIndex\":").append(zone.colorIndex)
            append(",\"exitBehavior\":").append(zone.exitBehavior)
            append(",\"alertMode\":").append(zone.alertMode)
            append(",\"scheduleEnabled\":").append(zone.scheduleEnabled)
            append(",\"scheduleDays\":").append(jsonString(zone.scheduleDays))
            append(",\"scheduleStartMin\":").append(zone.scheduleStartMin)
            append(",\"scheduleEndMin\":").append(zone.scheduleEndMin)
            append(",\"silentEnabled\":").append(zone.silentEnabled)
            append(",\"silentStartMin\":").append(zone.silentStartMin)
            append(",\"silentEndMin\":").append(zone.silentEndMin)
            append('}')
        }
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.toByteArray(Charsets.UTF_8))
    }

    /** رمزگشایی payload؛ در هر خطایی failure، نه throw. */
    fun decode(payload: String): Result<ZoneDraft> {
        return try {
            decodeOrThrow(payload)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Invalid zone share payload", e))
        }
    }

    private fun decodeOrThrow(payload: String): Result<ZoneDraft> {
        if (payload.isBlank()) return Result.failure(
            IllegalArgumentException("Empty payload")
        )
        val bytes = try {
            Base64.getUrlDecoder().decode(payload)
        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        }
        val json = String(bytes, Charsets.UTF_8)
        val fields = JsonParser(json).parseObject()
            ?: return Result.failure(IllegalArgumentException("Not a JSON object"))

        val name = fields["name"] as? String
            ?: return Result.failure(IllegalArgumentException("Missing name"))
        if (name.isBlank()) return Result.failure(
            IllegalArgumentException("Blank name")
        )
        val lat = fields["lat"] as? Double
        val lng = fields["lng"] as? Double
        val radius = fields["radiusMeters"] as? Double
        if (lat == null || !lat.isFinite() || lat < -90.0 || lat > 90.0) {
            return Result.failure(IllegalArgumentException("Bad latitude"))
        }
        if (lng == null || !lng.isFinite() || lng < -180.0 || lng > 180.0) {
            return Result.failure(IllegalArgumentException("Bad longitude"))
        }
        if (radius == null || !radius.isFinite() || radius <= 0.0 ||
            radius > MAX_RADIUS_METERS
        ) {
            return Result.failure(IllegalArgumentException("Bad radius"))
        }
        // فیلدهای اختیاری: نبودنشان مقدار پیش‌فرض Zone را می‌گیرد و
        // مقدارهای خارج از بازه به نزدیک‌ترین مقدار معتبر چفت می‌شوند
        // (نه رد کل پیوند)، چون رنگ/رفتار خروج/نوع هشدار، هویت محدوده نیستند.
        val scheduleDays = (fields["scheduleDays"] as? String)
            ?.takeIf { it.isValidScheduleDays() } ?: "1,2,3,4,5,6,7"
        val polygonJson = (fields["polygonJson"] as? String) ?: ""
        return Result.success(
            ZoneDraft(
                name = name,
                latitude = lat,
                longitude = lng,
                radiusMeters = radius,
                polygonJson = polygonJson,
                colorIndex = (fields["colorIndex"] as? Double)?.toInt()
                    ?.coerceIn(0, 11) ?: 0,
                exitBehavior = (fields["exitBehavior"] as? Double)?.toInt()
                    ?.takeIf { it in 0..2 } ?: Zone.EXIT_STOP,
                alertMode = (fields["alertMode"] as? Double)?.toInt()
                    ?.takeIf { it in 0..3 } ?: Zone.ALERT_BOTH,
                scheduleEnabled = fields["scheduleEnabled"] as? Boolean ?: false,
                scheduleDays = scheduleDays,
                scheduleStartMin = (fields["scheduleStartMin"] as? Double)?.toInt()
                    ?.coerceIn(0, 1439) ?: 0,
                scheduleEndMin = (fields["scheduleEndMin"] as? Double)?.toInt()
                    ?.coerceIn(0, 1439) ?: 1439,
                silentEnabled = fields["silentEnabled"] as? Boolean ?: false,
                silentStartMin = (fields["silentStartMin"] as? Double)?.toInt()
                    ?.coerceIn(0, 1439) ?: 1320,
                silentEndMin = (fields["silentEndMin"] as? Double)?.toInt()
                    ?.coerceIn(0, 1439) ?: 360
            )
        )
    }

    private fun String.isValidScheduleDays(): Boolean {
        if (isBlank()) return false
        return split(",").all { part ->
            val day = part.trim().toIntOrNull()
            day != null && day in 1..7
        }
    }

    /** عدد با نقطه اعشار لاتین؛ Double.toString مستقل از locale است. */
    private fun num(value: Double): String = value.toString()

    /** رشته JSON با escape کامل: کوتیشن، بک‌اسلش و کاراکترهای کنترلی. */
    private fun jsonString(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        sb.append(String.format(Locale.US, "\\u%04x", ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    /**
     * پارسِر کوچک JSON برای یک شیء سطح‌بالا: رشته، عدد، true/false/null
     * و (برای تحمل نسخه‌های آینده) آرایه/شیء تو در تو را می‌خواند ولی
     * مقدارشان را دور می‌اندازد. کلید تکراری: آخری می‌برد. هر خطای
     * نحوی → null (نه throw به بیرون decode).
     */
    private class JsonParser(private val text: String) {
        private var pos = 0
        private var failed = false

        fun parseObject(): Map<String, Any?>? {
            return try {
                skipWs()
                if (!consume('{')) return null
                val result = linkedMapOf<String, Any?>()
                skipWs()
                if (consume('}')) return result
                while (!failed) {
                    skipWs()
                    val key = parseString() ?: return null
                    skipWs()
                    if (!consume(':')) return null
                    val value = parseValue()
                    // nullِ JSON با خطای پارس فرق دارد: خطا پرچم failed را
                    // می‌زند، ولی null واقعی فقط مقدار فیلد است (فیلدهای
                    // اختیاری بعداً مقدار پیش‌فرض می‌گیرند).
                    if (failed) return null
                    result[key] = value
                    skipWs()
                    when {
                        consume(',') -> continue
                        consume('}') -> return result
                        else -> return null
                    }
                }
                return null
            } catch (e: Exception) {
                null
            }
        }

        /** مقدار؛ خطا پرچم failed را می‌زند و null برمی‌گرداند. */
        private fun parseValue(): Any? {
            skipWs()
            if (pos >= text.length) {
                failed = true
                return null
            }
            return when (text[pos]) {
                '"' -> parseString().also { if (it == null) failed = true }
                '{' -> {
                    // شیء تو در تو: خوانده و دور انداخته می‌شود.
                    pos++
                    skipNested('{', '}')
                    if (failed) null else PLACEHOLDER
                }
                '[' -> {
                    pos++
                    skipNested('[', ']')
                    if (failed) null else PLACEHOLDER
                }
                't' -> if (consumeLiteral("true")) true else null.also { failed = true }
                'f' -> if (consumeLiteral("false")) false else null.also { failed = true }
                'n' -> if (consumeLiteral("null")) null else null.also { failed = true }
                else -> parseNumber().also { if (it == null) failed = true }
            }
        }

        private fun skipNested(open: Char, close: Char) {
            var depth = 1
            val startFailed = failed
            var inString = false
            var escaped = false
            while (pos < text.length && depth > 0) {
                val ch = text[pos++]
                if (inString) {
                    if (escaped) {
                        escaped = false
                    } else if (ch == '\\') {
                        escaped = true
                    } else if (ch == '"') {
                        inString = false
                    }
                } else {
                    when (ch) {
                        '"' -> inString = true
                        open -> depth++
                        close -> depth--
                    }
                }
            }
            if (depth > 0 || inString) failed = true
            if (startFailed) failed = true
        }

        private fun parseString(): String? {
            if (pos >= text.length || text[pos] != '"') return null
            pos++
            val sb = StringBuilder()
            while (pos < text.length) {
                val ch = text[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= text.length) return null
                        when (val esc = text[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > text.length) return null
                                val hex = text.substring(pos, pos + 4)
                                val code = hex.toIntOrNull(16) ?: return null
                                pos += 4
                                sb.append(code.toChar())
                            }
                            else -> return null
                        }
                    }
                    else -> {
                        if (ch.code < 0x20) return null
                        sb.append(ch)
                    }
                }
            }
            return null
        }

        private fun parseNumber(): Double? {
            val start = pos
            if (pos < text.length && (text[pos] == '-' || text[pos] == '+')) pos++
            var digits = false
            while (pos < text.length && text[pos].isDigit()) {
                pos++
                digits = true
            }
            if (pos < text.length && text[pos] == '.') {
                pos++
                while (pos < text.length && text[pos].isDigit()) {
                    pos++
                    digits = true
                }
            }
            if (!digits) return null
            if (pos < text.length && (text[pos] == 'e' || text[pos] == 'E')) {
                var look = pos + 1
                if (look < text.length && (text[look] == '-' || text[look] == '+')) look++
                if (look < text.length && text[look].isDigit()) {
                    pos = look
                    while (pos < text.length && text[pos].isDigit()) pos++
                }
            }
            return text.substring(start, pos).toDoubleOrNull()
        }

        private fun consumeLiteral(literal: String): Boolean {
            if (text.startsWith(literal, pos)) {
                pos += literal.length
                return true
            }
            return false
        }

        private fun consume(ch: Char): Boolean {
            if (pos < text.length && text[pos] == ch) {
                pos++
                return true
            }
            return false
        }

        private fun skipWs() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        companion object {
            /** نگه‌دارنده مقدارهای تو در تو که برای ما مهم نیستند. */
            private val PLACEHOLDER = Any()
        }
    }
}
