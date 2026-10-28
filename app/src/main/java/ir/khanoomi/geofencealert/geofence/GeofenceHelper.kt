package ir.khanoomi.geofencealert.geofence

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.receiver.GpsStateReceiver
import ir.khanoomi.geofencealert.receiver.SelfHealReceiver
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

    /** پیشوند شناسه ژئوفنس «حلقه نزدیک شدن» برای هشدار قبل از ورود. */
    const val APPROACH_PREFIX = "approach_"

    /** سقف تعداد ژئوفنس هم‌زمان در Geofencing API. */
    const val MAX_GEOFENCES = 100

    fun requestId(id: Long): String = "$REQUEST_PREFIX$id"

    fun gateRequestId(id: Long): String = "$GATE_PREFIX$id"

    fun approachRequestId(id: Long): String = "$APPROACH_PREFIX$id"

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

    fun parseApproachId(requestId: String): Long? =
        if (requestId.startsWith(APPROACH_PREFIX)) {
            requestId.removePrefix(APPROACH_PREFIX).toLongOrNull()
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

        // خودترمیمی دوره‌ای: وقتی پایش روشن است زمان‌بندی و وقتی خاموش است لغو شود
        if (monitoringEnabled) {
            scheduleSelfHeal(context)
        } else {
            cancelSelfHeal(context)
        }

        if (monitoringEnabled && hasLocationPermission) {
            val zones = try {
                app.zoneRepo.getActiveZones().take(MAX_GEOFENCES)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot load active zones", e)
                emptyList()
            }
            // محدوده‌های چندضلعی (polygonJson غیرخالی) هرگز ژئوفنس سیستمی
            // نمی‌شوند — نه ژئوفنس عادی، نه دروازه، نه حلقه نزدیک شدن؛
            // آن‌ها فقط با پایش دقیق (ProximityMonitorService) ارزیابی می‌شوند.
            val circleZones = zones.filter { it.polygonJson.isBlank() }
            val threshold = ProximityMonitorService.PRECISE_RADIUS_THRESHOLD_M
            val normal = circleZones.filter { it.radiusMeters >= threshold }
            val gated = circleZones.filter { it.radiusMeters < threshold }
            if (normal.isNotEmpty()) {
                addZones(context, normal)
            }
            if (gated.isNotEmpty()) {
                addGateZones(context, gated)
            }
            // ژئوفنس‌های «نزدیک شدن»: حلقه بیرونی برای محدوده‌هایی که هشدار
            // نزدیک شدن فعال کرده‌اند (فاصله بیشتر از شعاع خود محدوده).
            // این ژئوفنس‌ها هم در سقف ۱۰۰تایی اندروید حساب می‌شوند.
            val approach = circleZones.filter {
                it.approachDistanceM > 0 &&
                    it.approachDistanceM.toDouble() > it.radiusMeters
            }
            if (approach.isNotEmpty()) {
                addApproachZones(context, approach)
            }
        } else {
            Log.i(
                TAG,
                "Skipping registration (monitoring=$monitoringEnabled, permission=$hasLocationPermission)"
            )
        }

        // هشدار «جی‌پی‌اس خاموش است»: اگر پایش روشن و مکان‌یابی گوشی خاموش
        // است اعلان هشدار نمایش داده شود و در غیر این صورت حذف شود. این
        // بررسی روی هر تازه‌سازی ثبت‌ها (از جمله لحظه روشن شدن پایش) اجرا
        // می‌شود تا کاربر بی‌درنگ خبردار شود.
        try {
            GpsStateReceiver.evaluate(context, monitoringEnabled)
        } catch (e: Exception) {
            Log.w(TAG, "GPS-off evaluation failed", e)
        }

        // همگام‌سازی سرویس پایش دقیق محدوده‌های کوچک (از هر نخی امن است)
        ProximityMonitorService.sync(context)
    }

    fun addZones(context: Context, zones: List<Zone>) {
        // دفاع دوم: چندضلعی‌ها حتی اگر مستقیم صدا زده شوند، ثبت نمی‌شوند.
        val circleZones = zones.filter { it.polygonJson.isBlank() }
        if (circleZones.isEmpty()) return
        val geofences = circleZones.map { zone ->
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
        // دفاع دوم: چندضلعی‌ها دروازه هم نمی‌گیرند (پایش دقیق‌شان بدون دروازه است).
        val circleZones = zones.filter { it.polygonJson.isBlank() }
        if (circleZones.isEmpty()) return
        val gateRadius = ProximityMonitorService.PRECISE_RADIUS_THRESHOLD_M.toFloat()
        val geofences = circleZones.map { zone ->
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

    /**
     * ثبت ژئوفنس‌های «نزدیک شدن»: حلقه بیرونی به شعاع دلخواه کاربر.
     * ورود به این حلقه در [GeofenceReceiver] به هشدار «نزدیک شدن» همان
     * محدوده ترجمه می‌شود؛ فقط رویداد ورود مهم است.
     */
    fun addApproachZones(context: Context, zones: List<Zone>) {
        // دفاع دوم: چندضلعی‌ها حلقه نزدیک شدن هم نمی‌گیرند.
        val circleZones = zones.filter { it.polygonJson.isBlank() }
        if (circleZones.isEmpty()) return
        val geofences = circleZones.map { zone ->
            Geofence.Builder()
                .setRequestId(approachRequestId(zone.id))
                .setCircularRegion(
                    zone.latitude,
                    zone.longitude,
                    zone.approachDistanceM.toFloat().coerceAtLeast(1f)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
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

    // ---- خودترمیمی پس‌زمینه (AlarmManager) ----

    private const val SELF_HEAL_REQUEST_CODE = 7701

    /** فاصله خودترمیمی دوره‌ای: هر ۶ ساعت (از نسخه ۲.۲.۰؛ قبلاً نیم‌روز بود). */
    private const val SELF_HEAL_INTERVAL_MS = 6 * 60 * 60 * 1000L

    private fun selfHealPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, SelfHealReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            SELF_HEAL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * زمان‌بندی بررسی دوره‌ای خودترمیمی (هر ۶ ساعت، غیر دقیق و کم‌مصرف).
     * وقتی پایش روشن است صدا زده می‌شود.
     */
    fun scheduleSelfHeal(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + SELF_HEAL_INTERVAL_MS,
                SELF_HEAL_INTERVAL_MS,
                selfHealPendingIntent(context)
            )
            Log.i(TAG, "Self-heal alarm scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "scheduleSelfHeal failed", e)
        }
    }

    /** لغو بررسی دوره‌ای خودترمیمی (وقتی پایش خاموش می‌شود). */
    fun cancelSelfHeal(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(selfHealPendingIntent(context))
            Log.i(TAG, "Self-heal alarm cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "cancelSelfHeal failed", e)
        }
    }
}
