package ir.khanoomi.geofencealert.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {

    @Query("SELECT * FROM zones ORDER BY sortOrder ASC, id ASC")
    fun observeZones(): Flow<List<Zone>>

    @Query("SELECT * FROM zones WHERE isActive = 1")
    suspend fun getActiveZones(): List<Zone>

    @Query("SELECT * FROM zones WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Zone?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(zone: Zone): Long

    @Delete
    suspend fun delete(zone: Zone)

    @Query("UPDATE zones SET isAlerting = :alerting WHERE id = :id")
    suspend fun setAlerting(id: Long, alerting: Boolean)

    @Query("UPDATE zones SET lastTriggeredAt = :ts WHERE id = :id")
    suspend fun updateLastTriggered(id: Long, ts: Long)

    @Query("UPDATE zones SET alertAcknowledged = :v WHERE id = :id")
    suspend fun setAcknowledged(id: Long, v: Boolean)

    @Query("UPDATE zones SET isActive = :v WHERE id = :id")
    suspend fun setActive(id: Long, v: Boolean)

    @Query("UPDATE zones SET insideSince = :v WHERE id = :id")
    suspend fun setInsideSince(id: Long, v: Long)

    @Query("UPDATE zones SET stayReminded = :v WHERE id = :id")
    suspend fun setStayReminded(id: Long, v: Boolean)

    @Query("UPDATE zones SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)

    /**
     * ثبت اتمیک ترتیب دستی تازه: همه ردیف‌ها در یک تراکنش به‌روز می‌شوند
     * تا ناظران فهرست فقط «یک بار» و با ترتیب نهایی مطلع شوند؛ وگرنه انتشار
     * میانیِ نیمه‌کاره، ترتیب را لحظه‌ای به‌هم‌ریخته نشان می‌داد.
     */
    @Transaction
    suspend fun updateSortOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("SELECT * FROM zones ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<Zone>

    @Query("DELETE FROM zones")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM zones WHERE isActive = 1")
    suspend fun countActive(): Int
}
