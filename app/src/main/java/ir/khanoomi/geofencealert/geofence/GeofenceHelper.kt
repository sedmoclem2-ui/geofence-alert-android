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
import ir.khanoomi.geofencealert.service.ProximityMonitorService
import kotlinx.coroutines.flow.first

/**
 * ثبت و حذف ژئوفنس‌ها در Geofencing API.
 *
 * - محدوده‌های با شعاع ۲۰۰ متر یا بیشتر: ژئوفنس عادی با همان شعاع.
 * - محدوده‌های کوچک‌تر: ژئوفنس «دروازه» ۲۰۰ متری؛ ورود به دروازه، پایش دقیق
 *   ([ProximityMonitorService]) را فعال و خروج از آن، پایش دقیق را متوقف می‌کند.
 *
 * نکته مهم: InitialTrigger تنظیم نمی‌شود، پس اگر گوشی هنگام ثبت، داخل محدوده باشد
 * هشداری فعال نمی‌شود؛ فقط «ورود جدید» رویداد ورود تولید می‌کند.
 */
object GeofenceHelper {
    private const val TAG = "GeofenceHelper"

    const val REQUEST_PREFIX = "zone_"

    /**
     * پیشوند شناسه ژئوفنس «دروازه» برای محدوده‌های کوچک.
     * محدوده‌های با شعاع کمتر از آستانه با یک ژئوفنس ۲۰۰ متری ثبت می‌شوند؛
     * ورود به این دروازه، پایش دقیق را فعال و خروج از آن، پایش دقیق را متوقف می‌کند.
     */
    const val GATE_PREFIX = "gate_"

    /** سقف تعداد ژئوفنس هم‌زمان در Geofencing API. */
    const val MAX_GEOFENCES = 100

    fun requestId(id: Long): String = "$REQUEST_PREFIX$id"

    fun gateRequestId(id: Long): String = "$GATE_PREFIX$id"

    fun parseId(requestId: String): Long? =
        if (requestId.startsWith(REQUEST_PREFIX)) {
            requestId.removePrefix(REQUEST_PREFIX).toLongOrNull()
        } else {
            null
        }

    fun parseGateId(requestId: String): Long? =
        if (requestId.startsWith(GATE_PREFIX)) {
            requestId.removePrefix(GATE_PREFIX).toLongOrNull()
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
     * - محدوده‌های فعال با شعاع ۲۰۰ متر یا بیشتر: ژئوفنس عادی با همان شعاع.
     * - محدوده‌های فعال با شعاع کمتر از ۲۰۰ متر: ژئوفنس «دروازه» ۲۰۰ متری؛
     *   ورود به دروازه پایش دقیق را شروع و خروج از آن پایش دقیق را متوقف می‌کند.
     * اگر پایش کلی فعال و مجوز مکان موجود نباشد، همه حذف می‌شوند.
     * در پایان، وضعیت سرویس پایش دقیق نیز همگام می‌شود.
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
        // وضعیت پایش دقیق قبلی با ثبت‌های جدید ناهماهنگ است؛ پاک شود
        ProximityMonitorService.clearArmed()

        if (monitoringEnabled && hasLocationPermission) {
            val zones = try {
                app.zoneRepo.getActiveZones().take(MAX_GEOFENCES)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot load active zones", e)
                emptyList()
            }
            val threshold = ProximityMonitorService.PRECISE_RADIUS_THRESHOLD_M
            val normal = zones.filter { it.radiusMeters >= threshold }
            val gated = zones.filter { it.radiusMeters < threshold }
            if (normal.isNotEmpty()) {
                addZones(context, normal)
            }
            if (gated.isNotEmpty()) {
                addGateZones(context, gated)
            }
        } else {
            Log.i(
                TAG,
                "Skipping registration (monitoring=$monitoringEnabled, permission=$hasLocationPermission)"
            )
        }

        // همگام‌سازی سرویس پایش دقیق محدوده‌های کوچک (از هر نخی امن است)
        ProximityMonitorService.sync(context)
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
        registerGeofences(context, geofences)
    }

    /**
     * ثبت ژئوفنس‌های «دروازه» ۲۰۰ متری برای محدوده‌های کوچک.
     * ورود/خروج از دروازه در [GeofenceReceiver] به فعال/غیرفعال شدن
     * پایش دقیق همان محدوده ترجمه می‌شود؛ خودِ دروازه هرگز هشدار نمی‌دهد.
     */
    fun addGateZones(context: Context, zones: List<Zone>) {
        if (zones.isEmpty()) return
        val gateRadius = ProximityMonitorService.PRECISE_RADIUS_THRESHOLD_M.toFloat()
        val geofences = zones.map { zone ->
            Geofence.Builder()
                .setRequestId(gateRequestId(zone.id))
                .setCircularRegion(zone.latitude, zone.longitude, gateRadius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }
        registerGeofences(context, geofences)
    }

    private fun registerGeofences(context: Context, geofences: List<Geofence>) {
        if (geofences.isEmpty()) return
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
