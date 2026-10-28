package ir.khanoomi.geofencealert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.service.AlertService
import ir.khanoomi.geofencealert.widget.SingleZoneWidgetProvider
import ir.khanoomi.geofencealert.widget.ZoneWidgetProvider
import ir.khanoomi.geofencealert.widget.RadarWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** تغییر وضعیت کلی پایش از ویجت یا کاشی تنظیمات سریع. */
class WidgetActionReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_TOGGLE_MONITORING -> toggleMonitoring(context)
                    ACTION_TOGGLE_ZONE -> toggleZone(context, intent)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun toggleMonitoring(context: Context) {
        try {
            val app = context.applicationContext as GeofenceAlertApp
            val cur = app.prefs.monitoringEnabled.first()
            app.prefs.setMonitoringEnabled(!cur)
            GeofenceHelper.refreshRegistrations(context)
            ZoneWidgetProvider.updateAll(context)
            RadarWidgetProvider.updateAll(context)
        } catch (e: Exception) {
            Log.e(TAG, "toggle monitoring failed", e)
        }
    }

    /** فعال/غیرفعال کردن یک محدوده مشخص (از ویجت تک‌محدوده). */
    private suspend fun toggleZone(context: Context, intent: Intent) {
        try {
            val zoneId = intent.getLongExtra(AlertService.EXTRA_ZONE_ID, -1L)
            if (zoneId == -1L) return
            val app = context.applicationContext as GeofenceAlertApp
            val zone = app.zoneRepo.getById(zoneId) ?: return
            // اگر هشدارش در حال پخش است، اول دستی متوقف شود.
            if (zone.isAlerting) {
                AlertController.stopAlert(context, zoneId, manual = true)
            }
            app.zoneRepo.setActive(zoneId, !zone.isActive)
            GeofenceHelper.refreshRegistrations(context)
            SingleZoneWidgetProvider.updateAll(context)
            ZoneWidgetProvider.updateAll(context)
            RadarWidgetProvider.updateAll(context)
        } catch (e: Exception) {
            Log.e(TAG, "toggle zone failed", e)
        }
    }

    companion object {
        /** معکوس کردن وضعیت پایش (فعال ⇄ غیرفعال). */
        const val ACTION_TOGGLE_MONITORING = "ir.khanoomi.geofencealert.action.TOGGLE_MONITORING"
        /** معکوس کردن وضعیت فعال بودن یک محدوده مشخص. */
        const val ACTION_TOGGLE_ZONE = "ir.khanoomi.geofencealert.action.TOGGLE_ZONE"
        private const val TAG = "WidgetActionReceiver"
    }
}
