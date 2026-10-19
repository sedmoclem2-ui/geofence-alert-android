package ir.khanoomi.geofencealert.data

import kotlinx.coroutines.flow.Flow

/** لایه دسترسی تمیز به محدوده‌ها؛ منطق تجاری این‌جا نیست، فقط پوشش روی DAO است. */
class ZoneRepository(private val dao: ZoneDao) {

    fun observeZones(): Flow<List<Zone>> = dao.observeZones()

    suspend fun getActiveZones(): List<Zone> = dao.getActiveZones()

    suspend fun getById(id: Long): Zone? = dao.getById(id)

    suspend fun upsert(zone: Zone): Long = dao.upsert(zone)

    suspend fun delete(zone: Zone) = dao.delete(zone)

    suspend fun setAlerting(id: Long, alerting: Boolean) = dao.setAlerting(id, alerting)

    suspend fun countActive(): Int = dao.countActive()
}
