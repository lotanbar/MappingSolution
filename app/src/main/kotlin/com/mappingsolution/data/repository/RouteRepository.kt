package com.mappingsolution.data.repository

import com.mappingsolution.data.db.dao.RouteDao
import com.mappingsolution.data.db.entity.RouteEntity
import com.mappingsolution.data.db.entity.RoutePointEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(private val dao: RouteDao) {

    fun observeAll(): Flow<List<RouteEntity>> = dao.observeAll()

    /** Returns routes where [RouteEntity.didUserTapStop] is false — incomplete recordings. */
    suspend fun getIncomplete(): List<RouteEntity> = dao.getIncomplete()

    suspend fun getById(id: Long): RouteEntity? = dao.getById(id)

    suspend fun insert(route: RouteEntity): Long = dao.insert(route)

    suspend fun updateFields(id: Long, name: String, description: String?, color: String) =
        dao.updateFields(id, name, description, color, updatedAt = System.currentTimeMillis())

    suspend fun update(route: RouteEntity) = dao.update(route.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(route: RouteEntity) = dao.delete(route)

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    suspend fun appendPoints(points: List<RoutePointEntity>) = dao.insertPoints(points)

    suspend fun getPoints(routeId: Long): List<RoutePointEntity> = dao.getPoints(routeId)
}
