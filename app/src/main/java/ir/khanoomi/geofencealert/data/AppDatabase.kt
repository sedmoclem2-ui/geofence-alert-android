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

/** مهاجرت ۴ به ۵: رفتار پس از خروج و تأیید دستی هشدار. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `alertAcknowledged` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `exitBehavior` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `postExitCount` INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `postExitDurationSec` INTEGER NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE `zones` ADD COLUMN `postExitGapSec` INTEGER NOT NULL DEFAULT 30")
        // نگاشت سیاست قدیمی: توقف خودکار → توقف فوری (0)، توقف دستی → ادامه تا توقف دستی (2)
        db.execSQL("UPDATE `zones` SET `exitBehavior` = CASE WHEN `stopMode` = 'MANUAL' THEN 2 ELSE 0 END")
    }
}

@Database(entities = [Zone::class, EventLog::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
    abstract fun eventDao(): EventDao
}
