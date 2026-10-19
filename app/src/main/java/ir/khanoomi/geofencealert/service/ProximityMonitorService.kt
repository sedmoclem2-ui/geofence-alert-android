package ir.khanoomi.geofencealert.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.GeoMath
import ir.khanoomi.geofencealert.geofence.ZoneTransitionHandler
import ir.khanoomi.geofencealert.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * سرویس پیش‌زمینه پایش دقیق محدوده‌های کوچک.
 *
 * ژئوفنس سیستمی اندروید برای شعاع‌های کمتر از ۲۰۰ متر دقیق نیست؛ این سرویس
 * برای محدوده‌های فعال با شعاع کمتر از [PRECISE_RADIUS_THRESHOLD_M]، هر
 * ۲۰ ثانیه فاصله دقیق گوشی تا مرکز محدوده را با فرمول هاورساین حساب می‌کند
 * و ورود/خروج را با همان منطق ژئوفنس سیستمی ([ZoneTransitionHandler]) اعلام می‌کند.
 *
 * سرویس فقط وقتی روشن می‌ماند که پایش کلی فعال، مجوز مکان موجود، و دست‌کم یک
 * محدوده فعال کوچک وجود داشته باشد؛ در غیر این صورت خودش را متوقف می‌کند.
 * چون سرویس پیش‌زمینه است، با بسته شدن برنامه (سوایپ) از بین نمی‌رود.
 */
class ProximityMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        Log.i(TAG, "ProximityMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SYNC) {
            scope.launch {
                try {
                    if (!needsPolling()) {
                        stopSelf()
                        return@launch
                    }
                    ensureLoopRunning()
                } catch (e: Exception) {
                    Log.e(TAG, "sync failed", e)
                }
            }
        }
        return START_STICKY
    }

    /** آیا شرایط پایش دقیق برقرار است؟ */
    private suspend fun needsPolling(): Boolean {
        val app = applicationContext as GeofenceAlertApp
        val monitoring = try {
            app.prefs.monitoringEnabled.first()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read prefs, assuming enabled", e)
            true
        }
        if (!monitoring) return false
        if (!hasLocationPermission()) return false
        val zones = try {
            app.zoneRepo.getActiveZones()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot load zones", e)
            emptyList()
        }
        return zones.any { it.radiusMeters < PRECISE_RADIUS_THRESHOLD_M }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** شروع حلقه ۲۰ ثانیه‌ای (اگر در حال اجرا نیست) به‌همراه اعلان پیش‌زمینه. */
    private fun ensureLoopRunning() {
        if (loopJob?.isActive == true) return
        try {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                Notifications.proximityNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            stopSelf()
            return
        }
        loopJob = scope.launch {
            val inside = mutableMapOf<Long, Boolean>()
            while (isActive) {
                try {
                    tick(inside)
                } catch (e: Exception) {
                    Log.e(TAG, "tick failed", e)
                }
                try {
                    if (!needsPolling()) {
                        stopSelf()
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "needsPolling check failed", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * یک دور بررسی: موقعیت فعلی → فاصله تا مرکز هر محدوده کوچک فعال →
     * تشخیص تغییر وضعیت داخل/خارج و اعلام ورود/خروج.
     *
     * اولین مشاهده هر محدوده فقط وضعیت اولیه را ثبت می‌کند (بدون هشدار)،
     * مثل رفتار ژئوفنس سیستمی که ثبتِ صرف، رویداد تولید نمی‌کند.
     */
    private suspend fun tick(inside: MutableMap<Long, Boolean>) {
        if (!needsPolling()) return
        val loc = getCurrentLocation() ?: return
        val app = applicationContext as GeofenceAlertApp
        val zones = try {
            app.zoneRepo.getActiveZones()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot load zones in tick", e)
            return
        }.filter { it.radiusMeters < PRECISE_RADIUS_THRESHOLD_M }
        for (zone in zones) {
            val d = GeoMath.distanceMeters(
                loc.latitude, loc.longitude, zone.latitude, zone.longitude
            )
            val isInside = d <= zone.radiusMeters
            when (val prev = inside[zone.id]) {
                null -> inside[zone.id] = isInside // مقداردهی اولیه بدون هشدار
                else -> {
                    if (!prev && isInside) {
                        inside[zone.id] = true
                        Log.i(TAG, "Precise ENTER for zone ${zone.id} (d=${d.toInt()}m)")
                        ZoneTransitionHandler.onEnter(this, zone)
                    } else if (prev && !isInside) {
                        inside[zone.id] = false
                        Log.i(TAG, "Precise EXIT for zone ${zone.id} (d=${d.toInt()}m)")
                        ZoneTransitionHandler.onExit(this, zone)
                    }
                }
            }
        }
        // پاک‌سازی وضعیت محدوده‌های حذف‌شده یا بزرگ‌شده
        val liveIds = zones.map { it.id }.toSet()
        inside.keys.retainAll(liveIds)
    }

    /** موقعیت فعلی با دقت بالا (مسدودکننده روی نخ IO؛ تا ۱۵ ثانیه صبر می‌کند). */
    private fun getCurrentLocation(): Location? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(this)
            val task = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            )
            Tasks.await(task, LOCATION_TIMEOUT_S, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w(TAG, "getCurrentLocation failed", e)
            null
        }
    }

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        Log.i(TAG, "ProximityMonitorService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ProximityMonitorService"

        /** آستانه شعاع به متر؛ کمتر از این، پایش دقیق جایگزین ژئوفنس سیستمی می‌شود. */
        const val PRECISE_RADIUS_THRESHOLD_M = 200.0

        const val ACTION_SYNC = "ir.khanoomi.geofencealert.action.PROXIMITY_SYNC"

        private const val FOREGROUND_NOTIFICATION_ID = 9003
        private const val POLL_INTERVAL_MS = 20_000L
        private const val LOCATION_TIMEOUT_S = 15L

        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * همگام‌سازی وضعیت سرویس با شرایط واقعی؛ امن برای فراخوانی از هر نخی.
         * اگر پایش دقیق لازم باشد سرویس را روشن، وگرنه خاموش می‌کند.
         */
        fun sync(context: Context) {
            syncScope.launch {
                try {
                    val app = context.applicationContext as GeofenceAlertApp
                    val monitoring = try {
                        app.prefs.monitoringEnabled.first()
                    } catch (e: Exception) {
                        true
                    }
                    val hasPerm =
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                    val zones: List<Zone> = try {
                        app.zoneRepo.getActiveZones()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val need = monitoring && hasPerm &&
                        zones.any { it.radiusMeters < PRECISE_RADIUS_THRESHOLD_M }
                    if (need) {
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, ProximityMonitorService::class.java)
                                .setAction(ACTION_SYNC)
                        )
                        Log.i(TAG, "sync: starting proximity monitoring")
                    } else {
                        context.stopService(
                            Intent(context, ProximityMonitorService::class.java)
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "sync failed", e)
                }
            }
        }
    }
}
