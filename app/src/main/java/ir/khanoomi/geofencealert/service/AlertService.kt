package ir.khanoomi.geofencealert.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.audio.AlertPlayer
import ir.khanoomi.geofencealert.audio.AlertVibrator
import ir.khanoomi.geofencealert.data.PrefsRepository
import ir.khanoomi.geofencealert.data.Zone
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

/**
 * سرویس پیش‌زمینه پخش هشدار صوتی.
 *
 * - نوع سرویس: location (اعلام‌شده در مانیفست).
 * - با ACTION_START: اعلان پیش‌زمینه نمایش داده و پخش حلقه‌ای شروع می‌شود.
 * - با ACTION_STOP: پخش آن محدوده متوقف می‌شود؛ اگر هشدار فعالی نماند، سرویس
 *   خودش را متوقف می‌کند.
 * - اگر سرویس توسط سیستم از بین برود درحالی‌که هشداری فعال است، برای هر
 *   محدوده فعال یک اعلان هشدار (با دکمه توقف) نمایش داده می‌شود تا کاربر
 *   همچنان بتواند صدا را قطع کند.
 */
class AlertService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeIds = mutableSetOf<Long>()
    private val activeNames = mutableMapOf<Long, String>()

    /** قفل بیداری صفحه هنگام هشدار (با مهلت خودکار آزاد می‌شود). */
    private var wakeLock: PowerManager.WakeLock? = null

    /** جاب چشمک چراغ قوه (در حالت «چشمک‌زن»). */
    private var blinkJob: Job? = null

    /** شناسه دوربین دارای فلش (یک بار پیدا و نگه داشته می‌شود). */
    private var torchCameraId: String? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Notifications.createChannels(this)
        Log.i(TAG, "AlertService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> handleStop(intent.getLongExtra(EXTRA_ZONE_ID, -1L))
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val zoneId = intent.getLongExtra(EXTRA_ZONE_ID, -1L)
        val zoneName = intent.getStringExtra(EXTRA_ZONE_NAME)
            ?: ir.khanoomi.geofencealert.ui.stringsForContext(this).zoneFallbackName
        val soundKey = intent.getStringExtra(EXTRA_SOUND_KEY) ?: "alarm"
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI)
        if (zoneId == -1L) {
            Log.w(TAG, "ACTION_START without zone id")
            return
        }
        synchronized(activeIds) {
            activeIds.add(zoneId)
            activeNames[zoneId] = zoneName
        }
        val notification = Notifications.alertNotification(this, zoneId, zoneName)
        try {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            // مسیر جایگزین: پخش مستقیم + اعلان عادی تا کاربر بی‌خبر نماند
            Notifications.notifyAlert(this, zoneId, zoneName)
        }
        // نوع هشدار، بلندی صدا و شدت لرزش از دیتابیس/تنظیمات خوانده می‌شود
        // تا حالت‌های «فقط لرزش» و «هیچ‌کدام» هم درست کار کنند.
        scope.launch {
            val app = applicationContext as GeofenceAlertApp
            val mode = try {
                app.zoneRepo.getById(zoneId)?.alertMode
            } catch (e: Exception) {
                Log.e(TAG, "getById failed for $zoneId", e)
                null
            } ?: Zone.ALERT_BOTH
            val volume = try {
                app.prefs.alertVolume.first() / 100f
            } catch (e: Exception) {
                0.8f
            }
            val vibIntensity = try {
                app.prefs.vibrationIntensity.first() / 100f
            } catch (e: Exception) {
                0.7f
            }
            // روشن شدن صفحه هنگام هشدار (اگر کاربر در تنظیمات خاموشش نکرده
            // باشد) تا اعلان روی صفحه قفل دیده شود.
            val wakeScreen = try {
                app.prefs.wakeScreenOnAlert.first()
            } catch (e: Exception) {
                true
            }
            if (wakeScreen) {
                acquireWakeLock()
            }
            // چراغ قوه هنگام هشدار طبق حالت انتخاب‌شده در تنظیمات
            // (خاموش/ثابت/چشمک‌زن).
            val flashMode = try {
                app.prefs.flashlightMode.first()
            } catch (e: Exception) {
                PrefsRepository.FLASHLIGHT_OFF
            }
            applyFlashlight(flashMode)
            if (AlertController.soundAllowed(mode)) {
                AlertPlayer.play(this@AlertService, zoneId, soundKey, soundUri, volume)
            }
            if (AlertController.vibrationAllowed(mode)) {
                AlertVibrator.start(this@AlertService, zoneId, vibIntensity)
            }
        }
    }

    private fun handleStop(zoneId: Long) {
        if (zoneId == -1L) return
        AlertPlayer.stop(zoneId)
        AlertVibrator.stop(this, zoneId)
        Notifications.cancelAlert(this, zoneId)
        val app = applicationContext as GeofenceAlertApp
        scope.launch {
            try {
                app.zoneRepo.setAlerting(zoneId, false)
            } catch (e: Exception) {
                Log.e(TAG, "setAlerting(false) failed for $zoneId", e)
            }
        }
        val stillActive = synchronized(activeIds) {
            activeIds.remove(zoneId)
            activeNames.remove(zoneId)
            activeIds.isNotEmpty()
        }
        if (!stillActive) {
            // آخرین هشدار هم تمام شد: چراغ قوه خاموش و قفل صفحه آزاد.
            stopFlashlight()
            releaseWakeLock()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * گرفتن قفل بیداری «روشن شدن صفحه» برای ۱۵ ثانیه هنگام شروع هشدار
     * (با ACQUIRE_CAUSES_WAKEUP صفحه قفل روشن می‌شود). مجوز WAKE_LOCK در
     * مانیفست هست و این قفل سقفی خودکار دارد؛ در پایان هشدار هم دستی آزاد
     * می‌شود.
     */
    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            releaseWakeLock()
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "GeofenceAlert:AlertWakeLock"
            ).apply { acquire(WAKE_LOCK_TIMEOUT_MS) }
        } catch (e: Exception) {
            Log.w(TAG, "acquireWakeLock failed", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    /**
     * اعمال حالت چراغ قوه انتخاب‌شده در تنظیمات. چراغ قوه (Torch) به مجوز
     * دوربین نیاز ندارد؛ روی گوشی بدون فلش یا دوربین مشغول، بی‌صدا رد
     * می‌شود.
     */
    private fun applyFlashlight(mode: String) {
        blinkJob?.cancel()
        blinkJob = null
        when (mode) {
            PrefsRepository.FLASHLIGHT_STEADY -> setTorch(true)
            PrefsRepository.FLASHLIGHT_BLINK -> {
                blinkJob = scope.launch {
                    var on = true
                    while (isActive) {
                        setTorch(on)
                        on = !on
                        delay(TORCH_BLINK_MS)
                    }
                }
            }
            else -> setTorch(false)
        }
    }

    /** توقف کامل چراغ قوه (پایان هشدار/سرویس): لغو چشمک + خاموش کردن چراغ. */
    private fun stopFlashlight() {
        blinkJob?.cancel()
        blinkJob = null
        setTorch(false)
    }

    private fun setTorch(on: Boolean) {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = torchCameraId
                ?: findTorchCamera(cm)?.also { torchCameraId = it }
                ?: return
            cm.setTorchMode(cameraId, on)
        } catch (e: Exception) {
            Log.w(TAG, "setTorch($on) failed", e)
        }
    }

    /** اولین دوربینی که فلش دارد (معمولاً دوربین پشت). */
    private fun findTorchCamera(cm: CameraManager): String? = try {
        cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: Exception) {
        Log.w(TAG, "findTorchCamera failed", e)
        null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // اگر سیستم سرویس را از بین برد، برای هشدارهای باقی‌مانده اعلان
        // عادی می‌گذاریم تا کاربر همچنان دکمه توقف را داشته باشد.
        // لرزش همه هشدارها هم متوقف می‌شود. چراغ قوه و قفل صفحه هم آزاد.
        stopFlashlight()
        releaseWakeLock()
        AlertVibrator.stopAll(this)
        val remaining: Map<Long, String> = synchronized(activeIds) {
            activeNames.toMap()
        }
        for ((id, name) in remaining) {
            Notifications.notifyAlert(this, id, name)
        }
        isRunning = false
        scope.cancel()
        Log.i(TAG, "AlertService destroyed")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AlertService"

        const val ACTION_START = "ir.khanoomi.geofencealert.action.START_ALERT"
        const val ACTION_STOP = "ir.khanoomi.geofencealert.action.STOP_ALERT"

        const val EXTRA_ZONE_ID = "zone_id"
        const val EXTRA_ZONE_NAME = "zone_name"
        const val EXTRA_SOUND_KEY = "sound_key"
        const val EXTRA_SOUND_URI = "sound_uri"

        private const val FOREGROUND_NOTIFICATION_ID = 9001

        /** مهلت قفل بیداری صفحه هنگام هشدار (۱۵ ثانیه). */
        private const val WAKE_LOCK_TIMEOUT_MS = 15_000L

        /** فاصله روشن/خاموش چراغ قوه در حالت چشمک‌زن (۶۰۰ میلی‌ثانیه). */
        private const val TORCH_BLINK_MS = 600L

        /** آیا سرویس در حال اجراست (برای تصمیم‌گیری درباره ارسال ACTION_STOP). */
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
