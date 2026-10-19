package ir.khanoomi.geofencealert.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.data.EventLog
import ir.khanoomi.geofencealert.domain.AlertDecider
import ir.khanoomi.geofencealert.domain.ScheduleChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * دریافت رویدادهای ورود/خروج از Geofencing API و هدایت به [AlertController].
 *
 * - ورود: فقط اگر محدوده فعال باشد و هم‌اکنون هشدارش پخش نشود → شروع هشدار.
 *   اگر زمان‌بندی محدوده خارج از بازه فعال باشد، هشدار شروع نمی‌شود (ولی
 *   رویداد در تاریخچه ثبت می‌گردد). در ساعات سکوت، هشدار بی‌صدا و فقط با
 *   اعلان نمایش داده می‌شود.
 * - خروج: فقط اگر حالت محدوده «توقف خودکار» باشد و هشدار در حال پخش باشد → توقف هشدار.
 * - ثبت محدوده به‌تنهایی (بدون ورود جدید) هشدار تولید نمی‌کند.
 */
class GeofenceReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: run {
            Log.w(TAG, "Null GeofencingEvent")
            return
        }
        if (event.hasError()) {
            Log.e(TAG, "GeofencingEvent error: ${event.errorCode}")
            return
        }
        val transition = event.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            return
        }
        val ids = event.triggeringGeofences
            ?.mapNotNull { GeofenceHelper.parseId(it.requestId) }
            .orEmpty()
        if (ids.isEmpty()) return

        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as GeofenceAlertApp
                for (id in ids) {
                    val zone = try {
                        app.zoneRepo.getById(id)
                    } catch (e: Exception) {
                        Log.e(TAG, "getById failed for $id", e)
                        null
                    } ?: continue
                    // ثبت رویداد در تاریخچه — برای هر ورود/خروج معتبر، حتی اگر هشدار
                    // به‌خاطر تکراری بودن شروع نشود.
                    if (zone.isActive) {
                        try {
                            val type = if (transition == Geofence.GEOFENCE_TRANSITION_ENTER)
                                EventLog.TYPE_ENTER else EventLog.TYPE_EXIT
                            app.db.eventDao().insert(
                                EventLog(zoneId = id, zoneName = zone.name, type = type)
                            )
                            app.db.eventDao().trim()
                            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                                app.zoneRepo.updateLastTriggered(id, System.currentTimeMillis())
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Event log failed for zone $id", e)
                        }
                    }
                    when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
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
                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
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
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
