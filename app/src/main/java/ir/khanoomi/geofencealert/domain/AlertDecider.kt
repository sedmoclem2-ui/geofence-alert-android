package ir.khanoomi.geofencealert.domain

import ir.khanoomi.geofencealert.data.Zone

/**
 * منطق خالص تصمیم‌گیری هشدار؛ بدون وابستگی به اندروید تا به‌راحتی تست شود.
 *
 * قوانین:
 * - ورود به محدوده «فعال» که هم‌اکنون هشدارش پخش نمی‌شود → شروع هشدار.
 * - خروج از محدوده فقط وقتی صدا را متوقف می‌کند که حالت محدوده «توقف خودکار هنگام خروج»
 *   باشد و هشدار واقعاً در حال پخش باشد.
 * - در حالت «توقف فقط دستی»، خروج هیچ اثری روی صدا ندارد.
 */
object AlertDecider {

    /** آیا با این رویداد ورود باید هشدار شروع شود؟ */
    fun shouldStartAlert(zone: Zone, alreadyAlerting: Boolean): Boolean =
        zone.isActive && !alreadyAlerting

    /** آیا با این رویداد خروج باید هشدار متوقف شود؟ */
    fun shouldStopOnExit(zone: Zone, isAlerting: Boolean): Boolean =
        isAlerting && zone.stopMode == Zone.STOP_AUTO
}
