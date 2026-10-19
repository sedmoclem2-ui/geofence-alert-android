package ir.khanoomi.geofencealert.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.khanoomi.geofencealert.alert.AlertController
import ir.khanoomi.geofencealert.service.AlertService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** توقف هشدار از طریق دکمه «توقف هشدار» در اعلان. */
class StopAlertReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val zoneId = intent.getLongExtra(AlertService.EXTRA_ZONE_ID, -1L)
        if (zoneId == -1L) return
        val pending = goAsync()
        scope.launch {
            try {
                AlertController.stopAlert(context, zoneId)
            } finally {
                pending.finish()
            }
        }
    }
}
