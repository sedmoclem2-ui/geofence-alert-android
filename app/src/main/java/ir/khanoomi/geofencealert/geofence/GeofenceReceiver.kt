package ir.khanoomi.geofencealert.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.domain.AlertDecider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * دریافت رویدادهای ورود/خروج از Geofencing API و هدایت به [AlertController].
 *
 * - ورود: فقط اگر محدوده فعال باشد و هم‌اکنون هشدارش پخش نشود → شروع هشدار.
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
                    when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            if (AlertDecider.shouldStartAlert(zone, zone.isAlerting)) {
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
                                AlertController.startAlert(context, fresh)
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
