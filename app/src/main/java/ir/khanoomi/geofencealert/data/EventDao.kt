package ir.khanoomi.geofencealert.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: EventLog)

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT 200")
    fun observeRecent(): Flow<List<EventLog>>

    @Query("DELETE FROM events")
    suspend fun clearAll()

    /** نگه‌داشتن حداکثر ۲۰۰ رکورد تازه؛ رکوردهای قدیمی‌تر حذف می‌شوند. */
    @Query("DELETE FROM events WHERE id NOT IN (SELECT id FROM events ORDER BY timestamp DESC LIMIT 200)")
    suspend fun trim()

    /** آمار تعداد ورود/خروج هر محدوده از یک زمان مشخص به بعد. */
    @Query("SELECT zoneId, zoneName, type, COUNT(*) AS count FROM events WHERE timestamp >= :since GROUP BY zoneId, zoneName, type")
    fun statsSince(since: Long): Flow<List<ZoneStat>>

    /** همه رویدادها از یک زمان مشخص به بعد (برای آمار ماهانه). */
    @Query("SELECT * FROM events WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getEventsSince(since: Long): List<EventLog>
}

/** یک ردیف آمار: تعداد رویدادهای یک نوع برای یک محدوده. */
data class ZoneStat(
    val zoneId: Long,
    val zoneName: String,
    val type: String,
    val count: Int
)
