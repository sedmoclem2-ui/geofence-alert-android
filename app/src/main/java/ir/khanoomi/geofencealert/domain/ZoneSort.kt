package ir.khanoomi.geofencealert.domain

import ir.khanoomi.geofencealert.data.Zone

/**
 * مرتب‌سازی فهرست محدوده‌ها بر اساس حالت انتخاب‌شده کاربر.
 * منطق خالص و بدون وابستگی به اندروید تا قابل تست واحد باشد.
 *
 * از نسخه ۲.۳.۰ حالت‌های جهت‌دار جدا شدند: نام (الفبا/برعکس)، ایجاد
 * (تازه‌ترین/قدیمی‌ترین) و شعاع (بزرگ‌ترین/کوچک‌ترین). مقادیر قدیمی
 * «name» و «radius» نسخه‌های قبل در [normalize] به معادل صعودی نگاشت
 * می‌شوند تا تنظیم ذخیره‌شده کاربر خراب نشود.
 */
object ZoneSort {

    /** ترتیب دستی کاربر (ستون sortOrder در دیتابیس). */
    const val MANUAL = "manual"

    /** بر اساس نام، ترتیب الفبا (صعودی). */
    const val NAME_ASC = "name_asc"

    /** بر اساس نام، برعکس الفبا (نزولی). */
    const val NAME_DESC = "name_desc"

    /** تازه‌ترین‌ها اول (شناسه بزرگ‌تر = ساخته‌شده دیرتر). */
    const val NEWEST = "newest"

    /** قدیمی‌ترین‌ها اول (شناسه کوچک‌تر = ساخته‌شده زودتر). */
    const val OLDEST = "oldest"

    /** بزرگ‌ترین شعاع اول. */
    const val RADIUS_DESC = "radius_desc"

    /** کوچک‌ترین شعاع اول. */
    const val RADIUS_ASC = "radius_asc"

    /** نام قدیمی حالت نام (نسخه‌های پیش از ۲.۳.۰)؛ معادل [NAME_ASC]. */
    const val NAME = NAME_ASC

    /** نام قدیمی حالت شعاع (نسخه‌های پیش از ۲.۳.۰)؛ معادل [RADIUS_ASC]. */
    const val RADIUS = RADIUS_ASC

    /**
     * نرمال‌سازی مقدار ذخیره‌شده: مقادیر قدیمی «name»/«radius» به معادل
     * صعودی و هر مقدار ناشناخته به «دستی» برگردانده می‌شود.
     */
    fun normalize(mode: String?): String = when (mode) {
        "name" -> NAME_ASC
        "radius" -> RADIUS_ASC
        MANUAL, NAME_ASC, NAME_DESC, NEWEST, OLDEST, RADIUS_DESC, RADIUS_ASC -> mode
        else -> MANUAL
    }

    fun sortZones(zones: List<Zone>, mode: String): List<Zone> = when (normalize(mode)) {
        NAME_ASC -> zones.sortedWith(
            compareBy({ it.name.lowercase() }, { it.id })
        )
        NAME_DESC -> zones.sortedWith(
            compareByDescending<Zone> { it.name.lowercase() }.thenBy { it.id }
        )
        NEWEST -> zones.sortedByDescending { it.id }
        OLDEST -> zones.sortedBy { it.id }
        RADIUS_DESC -> zones.sortedWith(
            compareByDescending<Zone> { it.radiusMeters }.thenBy { it.id }
        )
        RADIUS_ASC -> zones.sortedWith(
            compareBy({ it.radiusMeters }, { it.id })
        )
        else -> zones.sortedWith(
            compareBy({ it.sortOrder }, { it.id })
        )
    }
}
