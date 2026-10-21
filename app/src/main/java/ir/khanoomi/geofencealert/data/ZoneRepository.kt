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

    suspend fun updateLastTriggered(id: Long, ts: Long) = dao.updateLastTriggered(id, ts)

    suspend fun setAcknowledged(id: Long, v: Boolean) = dao.setAcknowledged(id, v)
    suspend fun setActive(id: Long, v: Boolean) = dao.setActive(id, v)

    suspend fun setInsideSince(id: Long, v: Long) = dao.setInsideSince(id, v)
    suspend fun setStayReminded(id: Long, v: Boolean) = dao.setStayReminded(id, v)

    suspend fun setSortOrder(id: Long, order: Int) = dao.setSortOrder(id, order)

    /** ثبت ترتیب دستی تازه در یک تراکنش واحد دیتابیس (بدون حالت نیم‌کاره). */
    suspend fun updateSortOrders(orderedIds: List<Long>) =
        dao.updateSortOrders(orderedIds)

    suspend fun getAll(): List<Zone> = dao.getAll()

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun countActive(): Int = dao.countActive()
}
