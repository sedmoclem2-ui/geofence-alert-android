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
}
