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
}
