package ir.khanoomi.geofencealert.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import android.util.Log
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.notify.Notifications
import ir.khanoomi.geofencealert.tile.MonitorTileService
import ir.khanoomi.geofencealert.widget.SingleZoneWidgetProvider
import ir.khanoomi.geofencealert.widget.ZoneWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * کنش «غیر فعال کردن پایش» از اعلان «جی‌پی‌اس خاموش است».
 *
 * پایش را از همان مسیر امنِ کلید اصلی خاموش می‌کند: ذخیره در تنظیمات،
 * تازه‌سازی ثبت ژئوفنس‌ها (که سرویس پایش دقیق و خودترمیمی را هم همگام
 * می‌کند)، حذف اعلان GPS و تازه‌سازی ویجت‌ها و کاشی تنظیمات سریع.
 * همه کارها روی نخ IO و داخل try/catch انجام می‌شود تا هیچ خطایی برنامه
 * را نکشد.
 */
class GpsActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISABLE_MONITORING) return
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as GeofenceAlertApp
                app.prefs.setMonitoringEnabled(false)
                GeofenceHelper.refreshRegistrations(context)
                Notifications.cancelGpsOff(context)
                ZoneWidgetProvider.updateAll(context)
                SingleZoneWidgetProvider.updateAll(context)
                try {
                    TileService.requestListeningState(
                        context,
                        ComponentName(context, MonitorTileService::class.java)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Tile refresh request failed", e)
                }
                Log.i(TAG, "Monitoring disabled from GPS-off notification")
            } catch (e: Exception) {
                Log.e(TAG, "Disable monitoring from notification failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** خاموش کردن پایش از اعلان «جی‌پی‌اس خاموش است». */
        const val ACTION_DISABLE_MONITORING =
            "ir.khanoomi.geofencealert.ACTION_DISABLE_MONITORING"
        private const val TAG = "GpsActionReceiver"
    }
}
