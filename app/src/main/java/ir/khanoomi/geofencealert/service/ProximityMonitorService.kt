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
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.domain.CheckInterval
import ir.khanoomi.geofencealert.domain.GeoMath
import ir.khanoomi.geofencealert.geofence.ZoneTransitionHandler
import ir.khanoomi.geofencealert.notify.Notifications
import ir.khanoomi.geofencealert.receiver.GpsStateReceiver
import ir.khanoomi.geofencealert.util.Permissions
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
import kotlin.math.max

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
 * منطق ژئوفنس سیستمی ([ZoneTransitionHandler]) اعلام می‌کند.
 *
 * از نسخه ۲.۲.۰ این سرویس تا وقتی «پایش» در تنظیمات روشن است همیشه زنده
 * می‌ماند (حتی وقتی هیچ محدوده‌ای armed نیست) تا بسته ماندن طولانی برنامه
 * باعث از دست رفتن هشدارها نشود؛ در حالت بیکار، اعلانش «پایش فعال است»
 * نشان می‌دهد و حلقه پایش دقیق نمی‌چرخد (مصرف باتری ناچیز). فقط خاموش
 * کردن پایش توسط کاربر، سرویس را متوقف می‌کند.
 *
 * چون سرویس پیش‌زمینه است، با بسته شدن برنامه (سوایپ) از بین نمی‌رود.
 */
class ProximityMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null
    private var tickerJob: Job? = null

    /**
     * آیا اعلان پیش‌زمینه فعلی «مرئی» است (پایش دقیق با ثانیه‌شمار) یا
     * «بی‌صدا» (حالت بیکار، کانال کمینه بدون آیکن نوار وضعیت). برای
     * جلوگیری از جابه‌جایی تکراری اعلان نگه داشته می‌شود.
     */
    @Volatile
    private var visibleNotification = false

    /** شنونده تغییر وضعیت مکان‌یابی گوشی برای اعلان «جی‌پی‌اس خاموش است». */
    private val gpsStateReceiver = GpsStateReceiver()

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        // تا وقتی این سرویس زنده است، خاموش/روشن شدن مکان‌یابی گوشی را
        // زیر نظر بگیر تا اعلان «جی‌پی‌اس خاموش است» به‌روز بماند.
        GpsStateReceiver.register(this, gpsStateReceiver)
        Log.i(TAG, "ProximityMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // قرارداد سرویس پیش‌زمینه: این سرویس همیشه با startForegroundService
        // شروع می‌شود، پس startForeground باید همین‌جا و هم‌زمان صدا زده
        // شود — پیش از هر کار ناهمگام. قبلاً این فراخوانی داخل
        // ensureLoopRunning و پس از خواندن تنظیمات/دیتابیس و گرفتن موقعیت
        // فعلی (Tasks.await تا ۱۵ ثانیه در coarseArmPass) انجام می‌شد و
        // وقتی مهلت ~۵ ثانیه‌ای سیستم می‌گذشت، اندروید با
        // ForegroundServiceDidNotStartInTimeException کل برنامه را می‌کشت؛
        // همچنین وقتی needsPolling نادرست بود، سرویس بدون startForeground
        // خودش را متوقف می‌کرد که باز هم همان کرش را می‌ساخت. نشانه‌اش
        // برای کاربر «هنگ و بسته شدن برنامه هنگام تغییر وضعیت پایش» بود.
        try {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                // حالت بیکار: اعلان بی‌صدای نامرئی (بدون آیکن نوار وضعیت).
                // اگر نشست دقیقی در جریان است، اعلان مرئی پایش دقیق می‌ماند
                // و ثانیه‌شمار ظرف یک ثانیه آن را تازه می‌کند.
                if (visibleNotification) {
                    Notifications.proximityNotification(this)
                } else {
                    Notifications.quietServiceNotification(this)
                },
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed in onStartCommand", e)
            stopSelf()
            return START_NOT_STICKY
        }
        // intent تهی یعنی سیستم پس از کشته شدن سرویس آن را دوباره ساخته است
        // (START_STICKY)؛ مثل ACTION_SYNC باهاش رفتار می‌شود تا وضعیت پایش
        // از نو همگام شود و سرویس بی‌جهت زامبی نماند.
        if (intent?.action == ACTION_SYNC || intent == null) {
            scope.launch {
                try {
                    if (!shouldStayAlive()) {
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

    /**
     * آیا سرویس باید زنده بماند؟ از نسخه ۲.۲.۰ شرط فقط روشن بودن پایش و
     * داشتن مجوز مکان است — وجود محدوده کوچک armed دیگر لازم نیست تا
     * سرویس (و اعلان «پایش فعال است») همیشه برقرار بماند.
     */
    private suspend fun shouldStayAlive(): Boolean {
        val app = applicationContext as GeofenceAlertApp
        val monitoring = try {
            app.prefs.monitoringEnabled.first()
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read prefs, assuming enabled", e)
            true
        }
        if (!monitoring) return false
        return hasLocationPermission()
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

    /**
     * شروع حلقه پایش (اگر در حال اجرا نیست).
     * اعلان پیش‌زمینه از قبل در [onStartCommand] به‌صورت هم‌زمان ثبت شده
     * است (الزام قرارداد startForegroundService)؛ اینجا فقط حلقه و
     * ثانیه‌شمار راه می‌افتند.
     */
    private fun ensureLoopRunning() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                try {
                    tickDue()
                } catch (e: Exception) {
                    Log.e(TAG, "tick failed", e)
                }
                if (armed.isEmpty()) {
                    // حالت بیکار: سرویس زنده می‌ماند (اعلان «پایش فعال است»)
                    // و فقط وضعیت پایش را هر ۳۰ ثانیه بازبینی می‌کند؛
                    // تنها وقتی کاربر پایش را خاموش کند متوقف می‌شود.
                    if (!shouldStayAlive()) {
                        stopSelf()
                        break
                    }
                    delay(IDLE_RECHECK_MS)
                    continue
                }
                val nextDelay = (armed.values.minOrNull()?.minus(System.currentTimeMillis()))
                    ?.coerceIn(1_000L, 3_600_000L) ?: 20_000L
                delay(nextDelay)
            }
        }
        ensureTickerRunning()
    }

    /**
     * ثانیه‌شمار و تعویض اعلان پیش‌زمینه:
     * - وقتی نشست پایش دقیق فعال است (محدوده‌ای armed و داخل دروازه‌ایم)،
     *   هر ثانیه نزدیک‌ترین موعد بررسی بعدی و نام محدوده‌اش روی اعلان
     *   «مرئی» پایش دقیق به‌روز می‌شود (دایره قرمز شمارش معکوس).
     * - وقتی هیچ نشست دقیقی فعال نیست، اعلان «بی‌صدای» کانال کمینه نشان
     *   داده می‌شود؛ اندروید برای اهمیت MIN هیچ آیکنی در نوار وضعیت نشان
     *   نمی‌دهد. تعویض بین دو حالت فقط هنگام تغییر وضعیت انجام می‌شود.
     */
    private fun ensureTickerRunning() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            val nm = NotificationManagerCompat.from(this@ProximityMonitorService)
            while (isActive) {
                try {
                    if (armed.isEmpty()) {
                        // پایان نشست دقیق: بازگشت به اعلان بی‌صدای نامرئی.
                        if (visibleNotification) {
                            visibleNotification = false
                            try {
                                ServiceCompat.startForeground(
                                    this@ProximityMonitorService,
                                    FOREGROUND_NOTIFICATION_ID,
                                    Notifications.quietServiceNotification(
                                        this@ProximityMonitorService
                                    ),
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "swap to quiet notification failed", e)
                            }
                        }
                        delay(1000)
                        continue
                    }
                    val now = System.currentTimeMillis()
                    val next = armed.minByOrNull { it.value }
                    if (next != null) {
                        val remaining = max(0, ((next.value - now) / 1000).toInt())
                        val app = applicationContext as GeofenceAlertApp
                        val name = try {
                            app.zoneRepo.getById(next.key)?.name
                        } catch (_: Exception) {
                            null
                        }
                        val notification = Notifications.proximityNotification(
                            this@ProximityMonitorService,
                            name ?: "محدوده",
                            remaining
                        )
                        if (!visibleNotification) {
                            // شروع نشست دقیق: اعلان مرئی جای اعلان بی‌صدا را
                            // می‌گیرد (با startForeground تا آیکن برگردد).
                            visibleNotification = true
                            try {
                                ServiceCompat.startForeground(
                                    this@ProximityMonitorService,
                                    FOREGROUND_NOTIFICATION_ID,
                                    notification,
                                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "swap to visible notification failed", e)
                                if (Permissions.hasNotifications(this@ProximityMonitorService)) {
                                    nm.notify(FOREGROUND_NOTIFICATION_ID, notification)
                                }
                            }
                        } else if (Permissions.hasNotifications(this@ProximityMonitorService)) {
                            // به‌روزرسانی ثانیه‌شمار فقط در حالت مرئی.
                            nm.notify(FOREGROUND_NOTIFICATION_ID, notification)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "ticker failed", e)
                }
                delay(1000)
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
            // حالت «مداوم» (۰) یعنی بررسی تقریباً پیوسته: هر ۲ ثانیه.
            val intervalMs = if (CheckInterval.isContinuous(zone.checkIntervalSec)) 2_000L
            else zone.checkIntervalSec.coerceIn(5, 3600) * 1000L
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
        GpsStateReceiver.unregister(this, gpsStateReceiver)
        loopJob?.cancel()
        tickerJob?.cancel()
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

        /** فاصله بازبینی وضعیت پایش در حالت بیکار (بدون محدوده armed). */
        private const val IDLE_RECHECK_MS = 30_000L

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
                // ورود تازه به دروازه یعنی عبور از مرز ۲۰۰ متری به داخل؛ پس در
                // لحظه ورود، بیرونِ شعاع دقیق هستیم (شعاع < ۲۰۰ متر). مقداردهی
                // false باعث می‌شود اولین تیکِ داخلِ شعاع، ورودِ تازه حساب شود.
                preciseInside[zoneId] = false
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
                    // از نسخه ۲.۲.۰ سرویس با خالی شدن armed متوقف نمی‌شود؛
                    // تا وقتی پایش روشن است زنده می‌ماند و اعلان «پایش فعال
                    // است» را نگه می‌دارد. توقف فقط از مسیر sync (خاموش
                    // شدن پایش یا از دست رفتن مجوز) انجام می‌شود.
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
         * از نسخه ۲.۲.۰: اگر پایش روشن و مجوز مکان موجود باشد سرویس همیشه
         * روشن می‌ماند (حتی بدون هیچ محدوده کوچک armed) تا بسته ماندن
         * طولانی برنامه هشدارها را از کار نیندازد؛ وگرنه خاموش می‌شود.
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
                    val need = monitoring && hasPerm
                    if (need) {
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, ProximityMonitorService::class.java)
                                .setAction(ACTION_SYNC)
                        )
                        Log.i(TAG, "sync: keeping monitoring service alive")
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
