package ir.khanoomi.geofencealert.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import ir.khanoomi.geofencealert.GeofenceAlertApp
import ir.khanoomi.geofencealert.geofence.GeofenceHelper
import ir.khanoomi.geofencealert.ui.stringsForContext
import ir.khanoomi.geofencealert.widget.ZoneWidgetProvider
import ir.khanoomi.geofencealert.widget.RadarWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * کاشی تنظیمات سریع برای فعال/غیرفعال کردن پایش محدوده‌ها از سایه اعلان.
 *
 * کاربر باید یک بار آن را دستی اضافه کند: تنظیمات سریع ← ویرایش (مداد) ←
 * کشیدن «محدوده هشدار» به بالا.
 */
class MonitorTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch {
            try {
                val app = applicationContext as GeofenceAlertApp
                val on = app.prefs.monitoringEnabled.first()
                withContext(Dispatchers.Main) {
                    qsTile?.let {
                        it.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        it.label = stringsForContext(applicationContext).appTitle
                        it.updateTile()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            try {
                val app = applicationContext as GeofenceAlertApp
                val cur = app.prefs.monitoringEnabled.first()
                app.prefs.setMonitoringEnabled(!cur)
                GeofenceHelper.refreshRegistrations(applicationContext)
                ZoneWidgetProvider.updateAll(applicationContext)
                RadarWidgetProvider.updateAll(applicationContext)
                withContext(Dispatchers.Main) {
                    qsTile?.let {
                        it.state = if (!cur) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        it.updateTile()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
