package ir.khanoomi.geofencealert.geofence

import android.content.Context
import android.util.Log
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.data.EventLog
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.AlertDecider
import ir.khanoomi.geofencealert.domain.ScheduleChecker

/**
 * منطق مشترک ورود/خروج به محدوده، مستقل از منبع رویداد
 * (ژئوفنس سیستمی یا پایش دقیق ۲۰ ثانیه‌ای).
 *
 * باید از داخل کانتکست IO (کوروتین) فراخوانی شود.
 */
object ZoneTransitionHandler {
    private const val TAG = "ZoneTransitionHandler"

    /**
     * منطق ورود: سفر تازه — تأیید دستی قبلی پاک می‌شود تا هشدار مثل اولین بار
     * پخش شود؛ سپس ثبت رویداد در تاریخچه، به‌روزرسانی زمان آخرین ورود،
     * گیت زمان‌بندی، ساعات سکوت، و شروع هشدار.
     */
    suspend fun onEnter(context: Context, zone: Zone) {
        val app = context.applicationContext as GeofenceAlertApp
        val id = zone.id
        // سفر جدید: هشدارِ دستیِ متوقف‌شده قبلی فراموش می‌شود.
        try {
            app.zoneRepo.setAcknowledged(id, false)
        } catch (e: Exception) {
            Log.e(TAG, "setAcknowledged(false) failed for zone $id", e)
        }
        // ثبت رویداد در تاریخچه — حتی اگر هشدار به‌خاطر تکراری بودن شروع نشود.
        if (zone.isActive) {
            try {
                app.db.eventDao().insert(
                    EventLog(zoneId = id, zoneName = zone.name, type = EventLog.TYPE_ENTER)
                )
                app.db.eventDao().trim()
                app.zoneRepo.updateLastTriggered(id, System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Event log failed for zone $id", e)
            }
        }
        if (AlertDecider.shouldStartAlert(zone, zone.isAlerting)) {
            // زمان‌بندی محدوده: خارج از بازه فعال، هشدار شروع نمی‌شود
            // (رویداد ورود بالاتر در تاریخچه ثبت شده است).
            val inSchedule = ScheduleChecker.isInSchedule(
                zone.scheduleEnabled,
                zone.scheduleDays,
                zone.scheduleStartMin,
                zone.scheduleEndMin
            )
            if (!inSchedule) {
                Log.i(TAG, "ENTER ignored for zone $id: outside schedule")
            } else {
                try {
                    app.zoneRepo.setAlerting(id, true)
                } catch (e: Exception) {
                    Log.e(TAG, "setAlerting failed", e)
                }
                val fresh = try {
                    app.zoneRepo.getById(id)
                } catch (e: Exception) {
                    null
                } ?: zone
                val quiet = try {
                    app.prefs.isQuietNow()
                } catch (e: Exception) {
                    Log.e(TAG, "isQuietNow failed", e)
                    false
                }
                AlertController.startAlert(context, fresh, silent = quiet)
            }
        } else {
            Log.i(
                TAG,
                "ENTER ignored for zone $id (active=${zone.isActive}, alerting=${zone.isAlerting})"
            )
        }
    }

    /**
     * منطق خروج: ثبت رویداد در تاریخچه، سپس رفتارِ پس از خروجِ همان محدوده:
     * - اگر کاربر هشدارِ این سفر را دستی متوقف کرده بود (تأییدشده)، فقط صدا
     *   قطع می‌شود و هیچ آلارم پس از خروجی پخش نمی‌گردد.
     * - توقف فوری: قطع هشدار.
     * - الگوی تکراری: قطع صدای داخل محدوده و شروع الگوی پس از خروج.
     * - ادامه تا توقف دستی: هیچ کاری (هشدار حلقه‌ای ادامه می‌یابد).
     */
    suspend fun onExit(context: Context, zone: Zone) {
        val app = context.applicationContext as GeofenceAlertApp
        val id = zone.id
        if (zone.isActive) {
            try {
                app.db.eventDao().insert(
                    EventLog(zoneId = id, zoneName = zone.name, type = EventLog.TYPE_EXIT)
                )
                app.db.eventDao().trim()
            } catch (e: Exception) {
                Log.e(TAG, "Event log failed for zone $id", e)
            }
        }
        // وضعیت تازه از دیتابیس؛ ممکن است پس از بارگذاری شیء zone تغییر کرده باشد.
        val fresh = try {
            app.zoneRepo.getById(id)
        } catch (e: Exception) {
            Log.e(TAG, "getById failed for $id", e)
            null
        } ?: zone
        if (fresh.alertAcknowledged) {
            Log.i(TAG, "EXIT for zone $id: acknowledged by user, stopping silently")
            AlertController.stopAlert(context, id, manual = false)
            return
        }
        when (fresh.exitBehavior) {
            Zone.EXIT_SEQUENCE -> {
                Log.i(TAG, "EXIT for zone $id: starting post-exit sequence")
                AlertController.stopAlert(context, id, manual = false)
                AlertController.startPostExitSequence(context, fresh)
            }
            Zone.EXIT_CONTINUE -> {
                Log.i(TAG, "EXIT for zone $id: continuing until manual stop")
                // عمداً هیچ کاری نمی‌شود؛ هشدار حلقه‌ای ادامه می‌یابد.
            }
            else -> { // Zone.EXIT_STOP
                AlertController.stopAlert(context, id, manual = false)
            }
        }
    }
}
