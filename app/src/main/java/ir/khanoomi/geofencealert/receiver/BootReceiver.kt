package ir.khanoomi.geofencealert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.service.ProximityMonitorService
import ir.khanoomi.geofencealert.widget.ZoneWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** پس از روشن شدن گوشی، ثبت ژئوفنس‌های فعال را بازیابی می‌کند. */
class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i(TAG, "Boot completed; refreshing geofence registrations")
        val pending = goAsync()
        scope.launch {
            try {
                GeofenceHelper.refreshRegistrations(context)
                ZoneWidgetProvider.updateAll(context)
                ProximityMonitorService.sync(context)
            } catch (e: Exception) {
                Log.e(TAG, "refreshRegistrations failed on boot", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
