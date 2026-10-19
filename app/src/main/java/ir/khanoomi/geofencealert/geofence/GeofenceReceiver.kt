package ir.khanoomi.geofencealert.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.service.ProximityMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * دریافت رویدادهای ورود/خروج از Geofencing API و هدایت به [ZoneTransitionHandler].
 *
 * فقط برای محدوده‌های با شعاع ۲۰۰ متر یا بیشتر؛ محدوده‌های کوچک‌تر با
 * پایش دقیق ۲۰ ثانیه‌ای ([ProximityMonitorService]) کنترل می‌شوند و
 * رویدادهای سیستمی آن‌ها نادیده گرفته می‌شود.
 *
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
                    // محدوده‌های کوچک‌تر از آستانه با پایش دقیق کنترل می‌شوند؛
                    // رویداد سیستمی آن‌ها (اگر رسید) نادیده گرفته می‌شود.
                    if (zone.radiusMeters < ProximityMonitorService.PRECISE_RADIUS_THRESHOLD_M) {
                        continue
                    }
                    when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER ->
                            ZoneTransitionHandler.onEnter(context, zone)
                        Geofence.GEOFENCE_TRANSITION_EXIT ->
                            ZoneTransitionHandler.onExit(context, zone)
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
