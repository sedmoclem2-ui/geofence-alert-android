package ir.khanoomi.geofencealert.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.service.ProximityMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * بیدارسازی دوره‌ای پایش دقیق در Doze.
 *
 * قفل بیداری جزئی نسخه ۳٫۹٫۸ برای صفحه قفل کافی نبود: وقتی گوشی به Doze
 * عمیق می‌رود، سیستم قفل‌های بیداری را نادیده می‌گیرد و پردازنده می‌خوابد؛
 * پس حلقه تیک (delay) و گرفتن موقعیت متوقف می‌شود و ورود اصلاً تشخیص داده
 * نمی‌شود (تست ۱۵ نوامبر ۲۰۲۶ با نسخه ۳٫۹٫۸: صفحه با دکمه پاور قفل شد،
 * ورود به محدوده ۵۰۰ متری اصلاً ثبت نشد). این گیرنده با
 * AlarmManager.setAndAllowWhileIdle هر ۲ دقیقه گوشی را از Doze بیدار
 * می‌کند تا سرویس پایش یک دور ارزیابی (tickDue) انجام دهد؛ بیدارسازی
 * خودتکرارشونده است: هر بار دریافت، بیدارسازی بعدی زمان‌بندی می‌شود.
 * زمان‌بندی فقط وقتی پایش روشن است برقرار است و با خاموش شدن پایش لغو
 * می‌شود؛ مسیر، همان مسیر خودترمیمی ([SelfHealReceiver]) است.
 */
class WakeupReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as GeofenceAlertApp
                val enabled = try {
                    app.prefs.monitoringEnabled.first()
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot read prefs, assuming enabled", e)
                    true
                }
                if (enabled) {
                    Log.i(TAG, "Wakeup: monitoring on, triggering check + rescheduling")
                    try {
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, ProximityMonitorService::class.java)
                                .setAction(ProximityMonitorService.ACTION_WAKEUP_CHECK)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Wakeup: start service failed", e)
                    }
                    schedule(context)
                } else {
                    Log.i(TAG, "Wakeup skipped: monitoring disabled, cancelling alarm")
                    cancel(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Wakeup failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "WakeupReceiver"

        private const val WAKEUP_REQUEST_CODE = 7702

        private fun wakeupPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, WakeupReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                WAKEUP_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * زمان‌بندی یک بیدارسازی تک‌بار (setAndAllowWhileIdle تا در Doze هم
         * تحویل شود؛ غیر دقیق و بدون نیاز به مجوز آلارم دقیق). پس از هر
         * دریافت، بیدارسازی بعدی از نو زمان‌بندی می‌شود.
         */
        fun schedule(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    WakeupTiming.nextTriggerAtMs(System.currentTimeMillis()),
                    wakeupPendingIntent(context)
                )
                Log.i(TAG, "Wakeup alarm scheduled")
            } catch (e: Exception) {
                Log.e(TAG, "schedule wakeup failed", e)
            }
        }

        /** لغو بیدارسازی دوره‌ای (وقتی پایش خاموش می‌شود). */
        fun cancel(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.cancel(wakeupPendingIntent(context))
                Log.i(TAG, "Wakeup alarm cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "cancel wakeup failed", e)
            }
        }
    }
}
