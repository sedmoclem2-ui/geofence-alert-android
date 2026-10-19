package ir.khanoomi.geofencealert.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.Zone
import kotlinx.coroutines.flow.first

/**
 * ثبت و حذف ژئوفنس‌ها در Geofencing API.
 *
 * نکته مهم: InitialTrigger تنظیم نمی‌شود، پس اگر گوشی هنگام ثبت، داخل محدوده باشد
 * هشداری فعال نمی‌شود؛ فقط «ورود جدید» رویداد ورود تولید می‌کند.
 */
object GeofenceHelper {
    private const val TAG = "GeofenceHelper"

    const val REQUEST_PREFIX = "zone_"

    /** سقف تعداد ژئوفنس هم‌زمان در Geofencing API. */
    const val MAX_GEOFENCES = 100

    fun requestId(id: Long): String = "$REQUEST_PREFIX$id"

    fun parseId(requestId: String): Long? =
        if (requestId.startsWith(REQUEST_PREFIX)) {
            requestId.removePrefix(REQUEST_PREFIX).toLongOrNull()
        } else {
            null
        }

    private fun geofencingClient(context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * همگام‌سازی ثبت ژئوفنس‌ها با وضعیت واقعی برنامه:
     * اگر پایش کلی فعال و مجوز مکان موجود باشد، همه حذف و محدوده‌های فعال
     * (حداکثر ۱۰۰ عدد) ثبت می‌شوند؛ در غیر این صورت همه حذف می‌شوند.
     */
    suspend fun refreshRegistrations(context: Context) {
        val app = context.applicationContext as GeofenceAlertApp
        val monitoringEnabled = try {
            app.prefs.monitoringEnabled.first()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read prefs, assuming enabled", e)
            true
        }
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        removeAll(context)

        if (monitoringEnabled && hasLocationPermission) {
            val zones = try {
                app.zoneRepo.getActiveZones().take(MAX_GEOFENCES)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot load active zones", e)
                emptyList()
            }
            if (zones.isNotEmpty()) {
                addZones(context, zones)
            }
        } else {
            Log.i(
                TAG,
                "Skipping registration (monitoring=$monitoringEnabled, permission=$hasLocationPermission)"
            )
        }
    }

    fun addZones(context: Context, zones: List<Zone>) {
        if (zones.isEmpty()) return
        val geofences = zones.map { zone ->
            Geofence.Builder()
                .setRequestId(requestId(zone.id))
                .setCircularRegion(
                    zone.latitude,
                    zone.longitude,
                    zone.radiusMeters.toFloat().coerceAtLeast(1f)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }
        val request = GeofencingRequest.Builder()
            .addGeofences(geofences)
            .build()
        try {
            geofencingClient(context)
                .addGeofences(request, geofencePendingIntent(context))
                .addOnSuccessListener { Log.i(TAG, "Registered ${geofences.size} geofences") }
                .addOnFailureListener { e -> Log.e(TAG, "addGeofences failed", e) }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing location permission for addGeofences", e)
        } catch (e: Exception) {
            Log.e(TAG, "addGeofences error", e)
        }
    }

    fun removeAll(context: Context) {
        try {
            geofencingClient(context)
                .removeGeofences(geofencePendingIntent(context))
                .addOnFailureListener { e -> Log.e(TAG, "removeGeofences failed", e) }
        } catch (e: Exception) {
            Log.e(TAG, "removeGeofences error", e)
        }
    }
}
