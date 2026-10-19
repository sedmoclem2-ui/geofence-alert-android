package ir.khanoomi.geofencealert.alert

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.audio.AlertPlayer
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.notify.Notifications
import ir.khanoomi.geofencealert.service.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * نقطه واحد شروع/توقف هشدارها.
 *
 * مسیر اصلی: [AlertService] (سرویس پیش‌زمینه).
 * مسیر جایگزین: اگر شروع سرویس پیش‌زمینه ممکن نباشد (مثلاً محدودیت شروع
 * سرویس در پس‌زمینه در اندروید ۱۲ به بالا یا نبود مجوز لازم)، مستقیم
 * [AlertPlayer] اجرا و اعلان هشدار نمایش داده می‌شود.
 *
 * توقف دستی هشدار، وضعیت «فعال بودن» محدوده را تغییر نمی‌دهد؛ این دو
 * مفهوم مستقل‌اند.
 */
object AlertController {
    private const val TAG = "AlertController"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** شروع هشدار برای یک محدوده (پس از رویداد ورود معتبر). */
    fun startAlert(context: Context, zone: Zone) {
        if (AlertPlayer.isPlaying(zone.id)) {
            Log.i(TAG, "Alert already playing for zone ${zone.id}; not starting another")
            return
        }
        val intent = Intent(context, AlertService::class.java)
            .setAction(AlertService.ACTION_START)
            .putExtra(AlertService.EXTRA_ZONE_ID, zone.id)
            .putExtra(AlertService.EXTRA_ZONE_NAME, zone.name)
            .putExtra(AlertService.EXTRA_SOUND_KEY, zone.soundKey)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.w(TAG, "Foreground service start failed; using fallback playback", e)
            AlertPlayer.play(context, zone.id, zone.soundKey)
            Notifications.notifyAlert(context, zone.id, zone.name)
        }
    }

    /** توقف هشدار یک محدوده؛ محدوده فعال می‌ماند و برای ورود بعدی آماده است. */
    fun stopAlert(context: Context, zoneId: Long) {
        AlertPlayer.stop(zoneId)
        Notifications.cancelAlert(context, zoneId)
        val app = context.applicationContext as GeofenceAlertApp
        scope.launch {
            try {
                app.zoneRepo.setAlerting(zoneId, false)
            } catch (e: Exception) {
                Log.e(TAG, "setAlerting(false) failed for $zoneId", e)
            }
        }
        if (AlertService.isRunning) {
            try {
                val intent = Intent(context, AlertService::class.java)
                    .setAction(AlertService.ACTION_STOP)
                    .putExtra(AlertService.EXTRA_ZONE_ID, zoneId)
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not deliver STOP to service", e)
            }
        }
    }
}
