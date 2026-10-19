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
     * منطق ورود: ثبت رویداد در تاریخچه، به‌روزرسانی زمان آخرین ورود،
     * گیت زمان‌بندی، ساعات سکوت، و شروع هشدار.
     */
    suspend fun onEnter(context: Context, zone: Zone) {
        val app = context.applicationContext as GeofenceAlertApp
        val id = zone.id
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
     * منطق خروج: ثبت رویداد در تاریخچه و توقف هشدار
     * در صورت سیاست «توقف خودکار با خروج».
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
        if (AlertDecider.shouldStopOnExit(zone, zone.isAlerting)) {
            AlertController.stopAlert(context, id)
        } else {
            Log.i(
                TAG,
                "EXIT ignored for zone $id (mode=${zone.stopMode}, alerting=${zone.isAlerting})"
            )
        }
    }
}
