package ir.khanoomi.geofencealert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.notify.Notifications
import ir.khanoomi.geofencealert.util.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * شنونده تغییر وضعیت مکان‌یابی گوشی (روشن/خاموش شدن GPS یا شبکه).
 *
 * به‌صورت runtime ثبت می‌شود (از سرویس پایش دقیق و از MainActivity)؛ با هر
 * تغییر وضعیت ارائه‌دهنده‌های مکان، وضعیت را دوباره ارزیابی می‌کند:
 * - پایش روشن + مکان‌یابی خاموش → اعلان «جی‌پی‌اس خاموش است» نمایش داده می‌شود.
 * - در غیر این صورت → اعلان حذف می‌شود.
 */
class GpsStateReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocationManager.PROVIDERS_CHANGED_ACTION) return
        val pending = goAsync()
        scope.launch {
            try {
                evaluate(context)
            } catch (e: Exception) {
                Log.e(TAG, "GPS state evaluation failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "GpsStateReceiver"

        /** ثبت runtime شنونده؛ خطا بی‌صدا نادیده گرفته می‌شود. */
        fun register(context: Context, receiver: GpsStateReceiver) {
            try {
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } catch (e: Exception) {
                Log.w(TAG, "register failed", e)
            }
        }

        /** لغو ثبت runtime شنونده. */
        fun unregister(context: Context, receiver: GpsStateReceiver) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w(TAG, "unregister failed", e)
            }
        }

        /**
         * ارزیابی وضعیت فعلی: خواندن «پایش فعال است؟» از تنظیمات و اعمال
         * قاعده نمایش/حذف اعلان GPS.
         */
        suspend fun evaluate(context: Context) {
            val app = context.applicationContext as GeofenceAlertApp
            val monitoring = try {
                app.prefs.monitoringEnabled.first()
            } catch (e: Exception) {
                Log.w(TAG, "Cannot read prefs, assuming monitoring off", e)
                false
            }
            evaluate(context, monitoring)
        }

        /**
         * اعمال قاعده اعلان GPS با وضعیت پایشِ معلوم: اگر پایش روشن و
         * مکان‌یابی گوشی خاموش است اعلان نشان داده می‌شود، وگرنه حذف.
         */
        fun evaluate(context: Context, monitoringEnabled: Boolean) {
            if (monitoringEnabled && !Permissions.isLocationEnabled(context)) {
                Notifications.notifyGpsOff(context)
            } else {
                Notifications.cancelGpsOff(context)
            }
        }
    }
}
