package ir.khanoomi.geofencealert.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.audio.AlertPlayer
import ir.khanoomi.geofencealert.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        val zoneName = intent.getStringExtra(EXTRA_ZONE_NAME) ?: "محدوده"
        val soundKey = intent.getStringExtra(EXTRA_SOUND_KEY) ?: "alarm"
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
            scope.launch { AlertPlayer.play(this@AlertService, zoneId, soundKey) }
            Notifications.notifyAlert(this, zoneId, zoneName)
            return
        }
        scope.launch { AlertPlayer.play(this@AlertService, zoneId, soundKey) }
    }

    private fun handleStop(zoneId: Long) {
        if (zoneId == -1L) return
        AlertPlayer.stop(zoneId)
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
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // اگر سیستم سرویس را از بین برد، برای هشدارهای باقی‌مانده اعلان
        // عادی می‌گذاریم تا کاربر همچنان دکمه توقف را داشته باشد.
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

        private const val FOREGROUND_NOTIFICATION_ID = 9001

        /** آیا سرویس در حال اجراست (برای تصمیم‌گیری درباره ارسال ACTION_STOP). */
        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
