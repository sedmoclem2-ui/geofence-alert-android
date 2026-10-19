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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * سرویس پیش‌زمینه پایش دقیق محدوده‌های کوچک.
 *
 * ژئوفنس سیستمی اندروید برای شعاع‌های کمتر از ۲۰۰ متر دقیق نیست؛ برای این
 * محدوده‌ها یک ژئوفنس «دروازه» ۲۰۰ متری ثبت می‌شود ([GeofenceHelper]):
 * - ورود به دروازه → [armZone](true): پایش دقیق این محدوده فعال می‌شود.
 * - خروج از دروازه → [armZone](false): پایش دقیق متوقف می‌شود.
 *
 * وقتی محدوده‌ای armed است، سرویس هر [Zone.checkIntervalSec] ثانیه فاصله دقیق
 * گوشی تا مرکز محدوده را با فرمول هاورساین حساب می‌کند و ورود/خروج را با همان
 * منطق ژئوفنس سیستمی ([ZoneTransitionHandler]) اعلام می‌کند. وقتی هیچ محدوده‌ای
 * armed نیست، سرویس خودش را متوقف می‌کند تا باتری مصرف نشود.
 *
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
                    coarseArmPass()
                    ensureLoopRunning()
                } catch (e: Exception) {
                    Log.e(TAG, "sync failed", e)
                }
            }
        }
        return START_STICKY
    }

    /** آیا شرایط کلی پایش دقیق برقرار است؟ */
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

    /**
     * یک دور درشت اولیه هنگام شروع سرویس: موقعیت فعلی گرفته می‌شود و هر
     * محدوده کوچکی که هم‌اکنون در ۲۰۰ متری است، armed می‌شود؛ وضعیت
     * داخل/خارج اولیه بی‌صدا ثبت می‌شود (بدون هشدار).
     */
    private suspend fun coarseArmPass() {
        val loc = getCurrentLocation() ?: return
        val app = applicationContext as GeofenceAlertApp
        val zones = try {
            app.zoneRepo.getActiveZones()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot load zones in coarse pass", e)
            return
        }.filter { it.radiusMeters < PRECISE_RADIUS_THRESHOLD_M }
        val now = System.currentTimeMillis()
        for (zone in zones) {
            val d = GeoMath.distanceMeters(
                loc.latitude, loc.longitude, zone.latitude, zone.longitude
            )
            if (d <= PRECISE_RADIUS_THRESHOLD_M) {
                armed[zone.id] = now
                preciseInside[zone.id] = d <= zone.radiusMeters
                Log.i(TAG, "Coarse-armed zone ${zone.id} (d=${d.toInt()}m)")
            }
        }
    }

    /** شروع حلقه پایش (اگر در حال اجرا نیست) به‌همراه اعلان پیش‌زمینه. */
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
            while (isActive) {
                try {
                    tickDue()
                } catch (e: Exception) {
                    Log.e(TAG, "tick failed", e)
                }
                if (armed.isEmpty()) {
                    stopSelf()
                    break
                }
                val nextDelay = (armed.values.minOrNull()?.minus(System.currentTimeMillis()))
                    ?.coerceIn(1_000L, 3_600_000L) ?: 20_000L
                delay(nextDelay)
            }
        }
    }

    /**
     * بررسی محدوده‌های armed که موعدشان رسیده؛ پس از هر بررسی، موعد بعدی
     * بر اساس [Zone.checkIntervalSec] همان محدوده تنظیم می‌شود.
     */
    private suspend fun tickDue() {
        val app = applicationContext as GeofenceAlertApp
        val zonesById = try {
            app.zoneRepo.getActiveZones().associateBy { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot load zones in tick", e)
            return
        }
        val now = System.currentTimeMillis()
        val due = armed.filter { it.value <= now }.keys.toList()
        for (id in due) {
            val zone = zonesById[id]
            if (zone == null || !zone.isActive ||
                zone.radiusMeters >= PRECISE_RADIUS_THRESHOLD_M
            ) {
                armed.remove(id)
                preciseInside.remove(id)
                continue
            }
            tickZone(zone)
            val intervalMs = zone.checkIntervalSec.coerceIn(5, 3600) * 1000L
            armed[id] = now + intervalMs
        }
    }

    /**
     * یک دور بررسی دقیق یک محدوده: فاصله واقعی تا مرکز و تشخیص
     * تغییر وضعیت داخل/خارج. اولین مشاهده فقط وضعیت را ثبت می‌کند.
     */
    private suspend fun tickZone(zone: Zone) {
        val loc = getCurrentLocation() ?: return
        val d = GeoMath.distanceMeters(
            loc.latitude, loc.longitude, zone.latitude, zone.longitude
        )
        val isInside = d <= zone.radiusMeters
        when (val prev = preciseInside[zone.id]) {
            null -> preciseInside[zone.id] = isInside // مقداردهی اولیه بدون هشدار
            else -> {
                if (!prev && isInside) {
                    preciseInside[zone.id] = true
                    Log.i(TAG, "Precise ENTER for zone ${zone.id} (d=${d.toInt()}m)")
                    ZoneTransitionHandler.onEnter(this, zone)
                } else if (prev && !isInside) {
                    preciseInside[zone.id] = false
                    Log.i(TAG, "Precise EXIT for zone ${zone.id} (d=${d.toInt()}m)")
                    ZoneTransitionHandler.onExit(this, zone)
                }
            }
        }
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
        private const val LOCATION_TIMEOUT_S = 15L

        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * محدوده‌های دارای پایش دقیق فعال: zoneId → زمان موعد بعدی (میلی‌ثانیه).
         * ورود به دروازه ۲۰۰ متری، محدوده را armed و خروج از آن، disarm می‌کند.
         */
        private val armed = ConcurrentHashMap<Long, Long>()

        /** آخرین وضعیت داخل/خارج شناخته‌شده هر محدوده armed. */
        private val preciseInside = ConcurrentHashMap<Long, Boolean>()

        /**
         * فعال/غیرفعال کردن پایش دقیق یک محدوده (از رویداد دروازه).
         * امن برای فراخوانی از هر نخی.
         */
        fun armZone(context: Context, zoneId: Long, arm: Boolean) {
            if (arm) {
                armed[zoneId] = System.currentTimeMillis() // بررسی فوری در دور بعد
                try {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, ProximityMonitorService::class.java)
                            .setAction(ACTION_SYNC)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "armZone: start service failed", e)
                }
                return
            }
            armed.remove(zoneId)
            syncScope.launch {
                try {
                    // اگر داخل شعاع کوچک بودیم، خروج ثبت شود
                    if (preciseInside.remove(zoneId) == true) {
                        val app = context.applicationContext as GeofenceAlertApp
                        val zone = try {
                            app.zoneRepo.getById(zoneId)
                        } catch (e: Exception) {
                            null
                        }
                        if (zone != null) {
                            Log.i(TAG, "Gate EXIT while inside → precise EXIT for $zoneId")
                            ZoneTransitionHandler.onExit(context, zone)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "armZone(false) failed", e)
                } finally {
                    if (armed.isEmpty()) {
                        try {
                            context.stopService(
                                Intent(context, ProximityMonitorService::class.java)
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "stopService failed", e)
                        }
                    }
                }
            }
        }

        /** پاک‌سازی کامل وضعیت پایش دقیق (مثلاً هنگام ثبت مجدد ژئوفنس‌ها). */
        fun clearArmed() {
            armed.clear()
            preciseInside.clear()
        }

        /**
         * همگام‌سازی وضعیت سرویس با شرایط واقعی؛ امن برای فراخوانی از هر نخی.
         * اگر پایش دقیق لازم باشد سرویس را روشن (تا درشت‌اولیه armed کند)،
         * وگرنه خاموش می‌کند.
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
