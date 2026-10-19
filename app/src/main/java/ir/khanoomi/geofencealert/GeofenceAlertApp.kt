package ir.khanoomi.geofencealert

import android.app.Application
import androidx.room.Room
import ir.khanoomi.geofencealert.data.AppDatabase
import ir.khanoomi.geofencealert.data.PrefsRepository
import ir.khanoomi.geofencealert.data.ZoneRepository
import ir.khanoomi.geofencealert.notify.Notifications

class GeofenceAlertApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }

    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "geofence_alert.db").build()
    }

    val zoneRepo: ZoneRepository by lazy { ZoneRepository(db.zoneDao()) }

    val prefs: PrefsRepository by lazy { PrefsRepository(this) }
}
