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
     * نرمال‌سازی ترتیب رأس‌های چندضلعی تا همیشه یک چندضلعی ساده
     * (بدون خودقطعی) ساخته شود — ریشه باگ «ضربدر به‌جای مستطیل»:
     * وصل کردن رأس‌ها به‌ترتیب خام ضربه‌ها (مثلاً قطر به قطر) یال‌های
     * متقاطع می‌سازد. مراحل: (الف) حذف تکراری‌های پشت‌سرهم (و نقطه
     * پایانیِ برابر با نقطه آغاز)؛ (ب) کمتر از ۳ رأس → ورودی دست‌نخورده
     * برمی‌گردد (فراخواننده آن را نامعتبر می‌داند)؛ (ج) مرتب‌سازی بر
     * حسب زاویه قطبی (atan2) دور مرکز ثقل — مرکز ثقلِ رأس‌ها همیشه
     * درون پوسته محدب است، پس این ترتیب چندضلعی‌ای ستاره‌ای نسبت به
     * مرکز و در نتیجه ساده است؛ (د) راستی‌آزمایی: اگر باز هم دو یال
     * ناهمسایه همدیگر را قطع کنند (ورودی تباه‌شده/هم‌خط)، پوسته محدب
     * (زنجیره یکنواخت) جایگزین می‌شود. تابع خالص و تکرارپذیر است:
     * نرمال‌سازیِ خروجیِ نرمال‌شده، همان خروجی را می‌دهد.
     */
    fun normalizePolygon(points: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        // (الف) حذف تکراری‌های پشت‌سرهم و نقطه پایانیِ برابر با آغاز.
        val deduped = mutableListOf<Pair<Double, Double>>()
        for (p in points) {
            if (deduped.isEmpty() || deduped.last() != p) deduped += p
        }
        while (deduped.size > 1 && deduped.first() == deduped.last()) {
            deduped.removeAt(deduped.size - 1)
        }
        // (ب) کمتر از ۳ رأس معتبر نیست؛ دست‌نخورده برگردد.
        if (deduped.size < 3) return points
        // (ج) مرتب‌سازی بر حسب زاویه دور مرکز ثقل.
        val (cLat, cLng) = centroid(deduped) ?: return points
        val sorted = deduped.sortedBy { (lat, lng) ->
            atan2(lat - cLat, lng - cLng)
        }
        // (د) راستی‌آزمایی خودقطعی؛ در صورت عبور، پوسته محدب.
        return if (hasCrossing(sorted)) convexHull(sorted) else sorted
    }

    /** آیا دو یال ناهمسایه چندضلعی همدیگر را قطع می‌کنند؟ */
    private fun hasCrossing(polygon: List<Pair<Double, Double>>): Boolean {
        val n = polygon.size
        for (i in 0 until n) {
            val a1 = polygon[i]
            val a2 = polygon[(i + 1) % n]
            for (j in i + 1 until n) {
                // یال‌های همسایه (و جفت اول/آخر که در یک رأس مشترک‌اند)
                // را نادیده بگیر: اشتراک‌شان در رأس، خودقطعی نیست.
                if (j == i + 1) continue
                if (i == 0 && j == n - 1) continue
                if (segmentsIntersect(a1, a2, polygon[j], polygon[(j + 1) % n])) return true
            }
        }
        return false
    }

    /** تقاطع دو پاره‌خط (شامل تماس و هم‌پوشانی هم‌خط) با آزمون جهت. */
    private fun segmentsIntersect(
        p1: Pair<Double, Double>, p2: Pair<Double, Double>,
        p3: Pair<Double, Double>, p4: Pair<Double, Double>
    ): Boolean {
        // مختصات: x = lng، y = lat.
        fun orient(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>): Double {
            return (b.second - a.second) * (c.first - a.first) -
                (b.first - a.first) * (c.second - a.second)
        }
        fun onSegment(a: Pair<Double, Double>, b: Pair<Double, Double>, c: Pair<Double, Double>): Boolean {
            return b.second in minOf(a.second, c.second)..maxOf(a.second, c.second) &&
                b.first in minOf(a.first, c.first)..maxOf(a.first, c.first)
        }
        val d1 = orient(p3, p4, p1)
        val d2 = orient(p3, p4, p2)
        val d3 = orient(p1, p2, p3)
        val d4 = orient(p1, p2, p4)
        if (((d1 > 0) != (d2 > 0)) && ((d3 > 0) != (d4 > 0))) return true
        if (d1 == 0.0 && onSegment(p3, p1, p4)) return true
        if (d2 == 0.0 && onSegment(p3, p2, p4)) return true
        if (d3 == 0.0 && onSegment(p1, p3, p2)) return true
        if (d4 == 0.0 && onSegment(p1, p4, p2)) return true
        return false
    }

    /** پوسته محدب با زنجیره یکنواخت (اندرو)؛ خروجی بدون تکرار نقطه پایانی. */
    private fun convexHull(points: List<Pair<Double, Double>>): List<Pair<Double, Double>> {
        // مرتب‌سازی بر حسب x (= lng) سپس y (= lat)؛ نقاط تکراری حذف.
        val sorted = points.distinct().sortedWith(
            compareBy<Pair<Double, Double>> { it.second }.thenBy { it.first }
        )
        if (sorted.size < 3) return sorted
        fun cross(o: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
            return (a.second - o.second) * (b.first - o.first) -
                (a.first - o.first) * (b.second - o.second)
        }
        val lower = mutableListOf<Pair<Double, Double>>()
        for (p in sorted) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), p) <= 0.0) {
                lower.removeAt(lower.size - 1)
            }
            lower += p
        }
        val upper = mutableListOf<Pair<Double, Double>>()
        for (p in sorted.reversed()) {
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), p) <= 0.0) {
                upper.removeAt(upper.size - 1)
            }
            upper += p
        }
        // نقطه اول و آخر هر زنجیره تکراری است؛ حذف می‌شوند.
        lower.removeAt(lower.size - 1)
        upper.removeAt(upper.size - 1)
        return lower + upper
    }

    /**
     * سریال‌سازی چندضلعی به همان قالب JSON پارس‌کننده: آرایه‌ای از
     * اشیای {lat,lng} با ۶ رقم اعشار؛ برای بازنویسی ترتیب نرمال‌شده
     * در ذخیره‌سازی استفاده می‌شود.
     */
    fun polygonToJson(polygon: List<Pair<Double, Double>>): String {
        return polygon.joinToString(",", "[", "]") { (lat, lng) ->
            val a = String.format(java.util.Locale.US, "%.6f", lat)
            val b = String.format(java.util.Locale.US, "%.6f", lng)
            "{\"lat\":$a,\"lng\":$b}"
        }
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
