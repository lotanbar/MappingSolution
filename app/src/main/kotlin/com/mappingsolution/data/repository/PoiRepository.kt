package com.mappingsolution.data.repository

import com.mappingsolution.data.db.dao.PoiDao
import com.mappingsolution.data.db.entity.PoiEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiRepository @Inject constructor(private val dao: PoiDao) {

    fun observeAll(): Flow<List<PoiEntity>> = dao.observeAll()

    fun observeByGroup(groupId: Long): Flow<List<PoiEntity>> = dao.observeByGroup(groupId)

    fun observeOrphans(): Flow<List<PoiEntity>> = dao.observeOrphans()

    suspend fun countByGroup(groupId: Long): Int = dao.countByGroupId(groupId)

    suspend fun getById(id: Long): PoiEntity? = dao.getById(id)

    suspend fun insert(poi: PoiEntity): Long = dao.insert(poi)

    suspend fun update(poi: PoiEntity) = dao.update(poi.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(poi: PoiEntity) = dao.delete(poi)

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    suspend fun orphan(ids: List<Long>) = dao.orphan(ids)

    suspend fun moveToGroup(ids: List<Long>, groupId: Long) = dao.moveToGroup(ids, groupId)
}
