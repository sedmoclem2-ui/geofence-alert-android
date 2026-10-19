package ir.khanoomi.geofencealert.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** مهاجرت ۱ به ۲: جدول تاریخچه رویدادها + ستون آخرین ورود در جدول محدوده‌ها. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `zoneId` INTEGER NOT NULL, `zoneName` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `lastTriggeredAt` INTEGER NOT NULL DEFAULT 0")
    }
}

/** مهاجرت ۲ به ۳: زمان‌بندی و صدای دلخواه هر محدوده. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `scheduleEnabled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `scheduleDays` TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `scheduleStartMin` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `scheduleEndMin` INTEGER NOT NULL DEFAULT 1439")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `customSoundUri` TEXT")
    }
}

/** مهاجرت ۳ به ۴: فاصله بررسی دقیق هر محدوده. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `checkIntervalSec` INTEGER NOT NULL DEFAULT 20")
    }
}

@Database(entities = [Zone::class, EventLog::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
    abstract fun eventDao(): EventDao
}
