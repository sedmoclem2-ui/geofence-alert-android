package ir.khanoomi.geofencealert.domain

import ir.khanoomi.geofencealert.data.Zone

/**
 * مرتب‌سازی فهرست محدوده‌ها بر اساس حالت انتخاب‌شده کاربر.
 * منطق خالص و بدون وابستگی به اندروید تا قابل تست واحد باشد.
 */
object ZoneSort {

    /** ترتیب دستی کاربر (ستون sortOrder در دیتابیس). */
    const val MANUAL = "manual"

    /** بر اساس نام، صعودی. */
    const val NAME = "name"

    /** تازه‌ترین‌ها اول (شناسه بزرگ‌تر = ساخته‌شده دیرتر). */
    const val NEWEST = "newest"

    /** کوچک‌ترین شعاع اول. */
    const val RADIUS = "radius"

    fun sortZones(zones: List<Zone>, mode: String): List<Zone> = when (mode) {
        NAME -> zones.sortedWith(
            compareBy({ it.name.lowercase() }, { it.id })
        )
        NEWEST -> zones.sortedByDescending { it.id }
        RADIUS -> zones.sortedWith(
            compareBy({ it.radiusMeters }, { it.id })
        )
        else -> zones.sortedWith(
            compareBy({ it.sortOrder }, { it.id })
        )
    }
}
