package ir.khanoomi.geofencealert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.service.ProximityMonitorService
import ir.khanoomi.geofencealert.widget.ZoneWidgetProvider
import ir.khanoomi.geofencealert.widget.RadarWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * بازیابی ثبت ژئوفنس‌ها پس از رویدادهای سیستمی‌ای که ممکن است ثبت‌ها را
 * از بین ببرند: روشن شدن گوشی، به‌روزرسانی خود برنامه و تغییر ساعت یا
 * منطقه زمانی گوشی. بدنه بازیابی برای همه یکسان است.
 */
class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        Log.i(TAG, "System event ${intent.action}; refreshing geofence registrations")
        val pending = goAsync()
        scope.launch {
            try {
                GeofenceHelper.refreshRegistrations(context)
                ZoneWidgetProvider.updateAll(context)
                RadarWidgetProvider.updateAll(context)
                ProximityMonitorService.sync(context)
            } catch (e: Exception) {
                Log.e(TAG, "refreshRegistrations failed on ${intent.action}", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"

        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
