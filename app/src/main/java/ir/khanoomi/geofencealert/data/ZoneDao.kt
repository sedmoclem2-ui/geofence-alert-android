package ir.khanoomi.geofencealert.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {

    @Query("SELECT * FROM zones ORDER BY updatedAt DESC")
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

    @Query("SELECT COUNT(*) FROM zones WHERE isActive = 1")
    suspend fun countActive(): Int
}
