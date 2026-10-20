package ir.khanoomi.geofencealert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.service.ProximityMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * خودترمیمی پس‌زمینه: هر نیم‌روز (AlarmManager inexact) وضعیت پایش را بررسی
 * می‌کند؛ اگر پایش فعال است، ثبت ژئوفنس‌ها را تازه و سرویس پایش دقیق را
 * همگام می‌کند تا حتی اگر سیستم/کاربر سرویس را کشته باشد، پایش برگردد.
 * زمان‌بندی در [GeofenceHelper.scheduleSelfHeal] هنگام روشن بودن پایش انجام
 * و در [GeofenceHelper.cancelSelfHeal] هنگام خاموش شدن لغو می‌شود.
 */
class SelfHealReceiver : BroadcastReceiver() {
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
                    Log.i(TAG, "Self-heal: refreshing registrations + proximity sync")
                    GeofenceHelper.refreshRegistrations(context)
                    ProximityMonitorService.sync(context)
                } else {
                    Log.i(TAG, "Self-heal skipped: monitoring disabled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Self-heal failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SelfHealReceiver"
    }
}
