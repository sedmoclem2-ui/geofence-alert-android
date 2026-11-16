package ir.khanoomi.geofencealert.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.GeoMath
import ir.khanoomi.geofencealert.service.ProximityMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * دریافت رویدادهای ورود/خروج از Geofencing API و هدایت به [ZoneTransitionHandler].
 *
 * دو نوع رویداد داریم:
 * - ژئوفنس عادی (محدوده‌های با شعاع ۲۰۰ متر یا بیشتر): ورود/خروج مستقیماً
 *   به منطق هشدار ([ZoneTransitionHandler]) می‌رسد.
 * - ژئوفنس «دروازه» (محدوده‌های کوچک‌تر): ورود به دروازه، پایش دقیق همان
 *   محدوده را فعال و خروج از آن، پایش دقیق را متوقف می‌کند؛ خودِ دروازه
 *   هرگز هشدار تولید نمی‌کند.
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
        val triggering = event.triggeringGeofences.orEmpty()
        val gateIds = triggering.mapNotNull { GeofenceHelper.parseGateId(it.requestId) }
        val approachIds = triggering.mapNotNull { GeofenceHelper.parseApproachId(it.requestId) }
        val ids = triggering.mapNotNull { GeofenceHelper.parseId(it.requestId) }
        if (gateIds.isEmpty() && ids.isEmpty() && approachIds.isEmpty()) return

        val pending = goAsync()
        scope.launch {
            try {
                // دروازه‌ها: فعال/غیرفعال کردن پایش دقیق (بدون هشدار مستقیم)
                for (gateId in gateIds) {
                    when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            Log.i(TAG, "Gate ENTER for zone $gateId → arming precise monitoring")
                            ProximityMonitorService.armZone(context, gateId, true)
                        }
                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            Log.i(TAG, "Gate EXIT for zone $gateId → disarming precise monitoring")
                            ProximityMonitorService.armZone(context, gateId, false)
                        }
                    }
                }
                val app = context.applicationContext as GeofenceAlertApp
                // نزدیک شدن: فقط ورود به حلقه بیرونی رویداد دارد.
                if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    for (approachId in approachIds) {
                        val zone = try {
                            app.zoneRepo.getById(approachId)
                        } catch (e: Exception) {
                            Log.e(TAG, "getById failed for approach $approachId", e)
                            null
                        } ?: continue
                        if (!zone.isActive || zone.approachDistanceM <= 0) continue
                        Log.i(TAG, "APPROACH for zone $approachId")
                        ZoneTransitionHandler.onApproach(context, zone)
                    }
                }
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
                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            val freshLoc = fetchFreshLocation(context)
                            if (freshLoc != null && isLocationInsideZone(freshLoc, zone)) {
                                Log.i(TAG, "ignoring spurious system EXIT for zone ${zone.id}")
                            } else {
                                ZoneTransitionHandler.onExit(context, zone)
                            }
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun fetchFreshLocation(context: Context): Location? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val task = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            )
            Tasks.await(task, EXIT_VERIFY_TIMEOUT_S, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w(TAG, "fresh fix for EXIT check failed", e)
            null
        }
    }

    private fun isLocationInsideZone(loc: Location, zone: Zone): Boolean {
        return if (zone.polygonJson.isNotBlank()) {
            val polygon = GeoMath.normalizePolygon(GeoMath.parsePolygonJson(zone.polygonJson))
            GeoMath.pointInPolygon(loc.latitude, loc.longitude, polygon)
        } else {
            GeoMath.distanceMeters(
                loc.latitude, loc.longitude, zone.latitude, zone.longitude
            ) <= zone.radiusMeters
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val EXIT_VERIFY_TIMEOUT_S = 10L
    }
}
