package ir.khanoomi.geofencealert.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.audio.AlertVibrator
import ir.khanoomi.geofencealert.data.Zone
import ir.khanoomi.geofencealert.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * زمان‌بندی یادآور «ماندن داخل محدوده» با AlarmManager.
 * هر محدوده یک آلارم یکتا دارد (کد درخواست بر اساس شناسه محدوده).
 */
object StayAlarm {
    private const val TAG = "StayAlarm"
    const val ACTION_STAY_REMINDER = "ir.khanoomi.geofencealert.action.STAY_REMINDER"
    const val EXTRA_ZONE_ID = "stay_zone_id"

    private fun requestCode(zoneId: Long): Int = (10000 + (zoneId % 100000)).toInt()

    private fun pendingIntent(context: Context, zoneId: Long): PendingIntent {
        val intent = Intent(context, StayReminderReceiver::class.java)
            .setAction(ACTION_STAY_REMINDER)
            .putExtra(EXTRA_ZONE_ID, zoneId)
        return PendingIntent.getBroadcast(
            context,
            requestCode(zoneId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** زمان‌بندی یادآور برای [triggerAtMillis] (زمان مطلق دستگاه). */
    fun schedule(context: Context, zoneId: Long, triggerAtMillis: Long) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent(context, zoneId)
            )
            Log.i(TAG, "Stay reminder scheduled for zone $zoneId at $triggerAtMillis")
        } catch (e: Exception) {
            Log.e(TAG, "schedule failed for zone $zoneId", e)
        }
    }

    /** لغو یادآور یک محدوده (مثلاً هنگام خروج از محدوده). */
    fun cancel(context: Context, zoneId: Long) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pendingIntent(context, zoneId))
        } catch (e: Exception) {
            Log.e(TAG, "cancel failed for zone $zoneId", e)
        }
    }
}

/**
 * دریافت یادآور ماندن: اگر کاربر هنوز داخل محدوده است و برای این اقامت
 * قبلاً یادآوری نشده، یک اعلان («یادآور ماندن») به‌همراه لرزش کوتاه
 * (اگر نوع هشدار محدوده اجازه دهد) ارسال می‌شود؛ بدون صدا.
 */
class StayReminderReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != StayAlarm.ACTION_STAY_REMINDER) return
        val zoneId = intent.getLongExtra(StayAlarm.EXTRA_ZONE_ID, -1L)
        if (zoneId == -1L) return
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as GeofenceAlertApp
                val zone = try {
                    app.zoneRepo.getById(zoneId)
                } catch (e: Exception) {
                    Log.e(TAG, "getById failed for $zoneId", e)
                    null
                } ?: return@launch
                if (zone.insideSince <= 0L || zone.stayReminded) {
                    Log.i(TAG, "Stay reminder skipped for zone $zoneId (not inside or already reminded)")
                    return@launch
                }
                Notifications.stayReminderNotification(
                    context, zone.id, zone.name, zone.stayReminderMin
                )
                if (AlertController.vibrationAllowed(zone.alertMode)) {
                    val intensity = try {
                        app.prefs.vibrationIntensity.first() / 100f
                    } catch (e: Exception) {
                        0.7f
                    }
                    AlertVibrator.pulse(context, intensity)
                }
                try {
                    app.zoneRepo.setStayReminded(zoneId, true)
                } catch (e: Exception) {
                    Log.e(TAG, "setStayReminded failed for $zoneId", e)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "StayReminderReceiver"
    }
}
