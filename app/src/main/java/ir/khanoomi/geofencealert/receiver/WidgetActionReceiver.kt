package ir.khanoomi.geofencealert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.widget.ZoneWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** تغییر وضعیت کلی پایش از ویجت یا کاشی تنظیمات سریع. */
class WidgetActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_MONITORING) return
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as GeofenceAlertApp
                val cur = app.prefs.monitoringEnabled.first()
                app.prefs.setMonitoringEnabled(!cur)
                GeofenceHelper.refreshRegistrations(context)
                ZoneWidgetProvider.updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "toggle monitoring failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** معکوس کردن وضعیت پایش (فعال ⇄ غیرفعال). */
        const val ACTION_TOGGLE_MONITORING = "ir.khanoomi.geofencealert.action.TOGGLE_MONITORING"
        private const val TAG = "WidgetActionReceiver"
    }
}
