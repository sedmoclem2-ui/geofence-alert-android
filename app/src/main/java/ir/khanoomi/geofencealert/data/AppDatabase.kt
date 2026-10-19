package ir.khanoomi.geofencealert.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Zone::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
}
