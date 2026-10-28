package ir.khanoomi.geofencealert.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** محاسبات جغرافیایی خالص (بدون وابستگی به اندروید). */
object GeoMath {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /**
     * فاصله دقیق دو نقطه جغرافیایی به متر (فرمول هاورساین).
     * برای پایش دقیق محدوده‌های کوچک (شعاع کمتر از ۲۰۰ متر) استفاده می‌شود.
     */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    // ---- چندضلعی (بدون وابستگی به اندروید؛ پارس دستی تا در تست واحد JVM هم کار کند) ----

    /** تلرانس مرزی بر حسب درجه (حدود ۰٫۱ میلی‌متر) برای تشخیص «روی رأس/یال». */
    private const val BOUNDARY_EPS = 1e-9

    /**
     * آیا نقطه داخل چندضلعی است؟ الگوریتم پرتاب پرتو (ray casting) با
     * lat به‌عنوان y و lng به‌عنوان x؛ رأس‌های چندضلعی به‌ترتیب
     * (lat, lng) داده می‌شوند.
     *
     * - کمتر از ۳ رأس → همیشه false (چندضلعی تباه‌شده).
     * - نقطه دقیقاً روی یک رأس یا روی یک یال → داخل (true) محسوب می‌شود.
     */
    fun pointInPolygon(lat: Double, lng: Double, polygon: List<Pair<Double, Double>>): Boolean {
        if (polygon.size < 3) return false
        val y = lat
        val x = lng
        // مرز: روی رأس یا روی یال → داخل.
        for (i in polygon.indices) {
            val (lat1, lng1) = polygon[i]
            val (lat2, lng2) = polygon[(i + 1) % polygon.size]
            if (isOnSegment(y, x, lat1, lng1, lat2, lng2)) return true
        }
        // پرتاب پرتو به سمت +x و شمارش قطع‌ها (قاعده زوج/فرد).
        var inside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val (yi, xi) = polygon[i]
            val (yj, xj) = polygon[j]
            if ((yi > y) != (yj > y)) {
                val xIntersect = (xj - xi) * (y - yi) / (yj - yi) + xi
                if (x < xIntersect) inside = !inside
            }
            j = i
        }
        return inside
    }

    /** آیا نقطه (y=lat, x=lng) روی پاره‌خط (y1,x1)-(y2,x2) است (با تلرانس)؟ */
    private fun isOnSegment(
        y: Double, x: Double,
        y1: Double, x1: Double,
        y2: Double, x2: Double
    ): Boolean {
        // هم‌خطی: ضرب خارجی نزدیک صفر.
        val cross = (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1)
        if (kotlin.math.abs(cross) > BOUNDARY_EPS) return false
        // درون بازه مستطیلی پاره‌خط (با تلرانس).
        val minY = minOf(y1, y2) - BOUNDARY_EPS
        val maxY = maxOf(y1, y2) + BOUNDARY_EPS
        val minX = minOf(x1, x2) - BOUNDARY_EPS
        val maxX = maxOf(x1, x2) + BOUNDARY_EPS
        return y in minY..maxY && x in minX..maxX
    }

    /**
     * پارس JSON چندضلعی: آرایه‌ای از اشیای {lat,lng}، مثلاً
     * `[{"lat":35.7,"lng":51.4},{"lat":35.8,"lng":51.4}]`.
     * ترتیب کلیدها مهم نیست و فیلدهای اضافی نادیده گرفته می‌شوند.
     * ورودی خالی، نامعتبر یا ناقص (شیء بدون lat یا lng) → فهرست خالی.
     * هرگز استثنا پرتاب نمی‌کند.
     */
    fun parsePolygonJson(json: String): List<Pair<Double, Double>> {
        return try {
            parsePolygonJsonOrNull(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parsePolygonJsonOrNull(json: String): List<Pair<Double, Double>>? {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null
        val body = trimmed.substring(1, trimmed.length - 1).trim()
        if (body.isEmpty()) return emptyList()
        // هر شیء {…} باید دقیقاً یک lat و یک lng عددی داشته باشد.
        val objectRegex = Regex("""\{[^}]*\}""")
        val latRegex = Regex(""""lat"\s*:\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)""")
        val lngRegex = Regex(""""lng"\s*:\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)""")
        val objects = objectRegex.findAll(body).toList()
        if (objects.isEmpty()) return null
        // محتوای خارج از شیءها جز ویرگول و فاصله نباید چیزی باشد.
        val outside = objectRegex.replace(body, "").replace(",", "").trim()
        if (outside.isNotEmpty()) return null
        val result = mutableListOf<Pair<Double, Double>>()
        for (match in objects) {
            val text = match.value
            val lat = latRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            val lng = lngRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
            if (lat == null || lng == null || !lat.isFinite() || !lng.isFinite()) return null
            result += lat to lng
        }
        return result
    }

    /**
     * مرکز ثقل چندضلعی = میانگین رأس‌ها (نه مرکز ثقل مساحتی؛ برای
     * نمایش/ویرایش در نقشه کافی است). چندضلعی خالی → null.
     */
    fun centroid(polygon: List<Pair<Double, Double>>): Pair<Double, Double>? {
        if (polygon.isEmpty()) return null
        var sumLat = 0.0
        var sumLng = 0.0
        for ((lat, lng) in polygon) {
            sumLat += lat
            sumLng += lng
        }
        return (sumLat / polygon.size) to (sumLng / polygon.size)
    }
}
