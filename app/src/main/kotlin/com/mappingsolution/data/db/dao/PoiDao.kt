package com.mappingsolution.data.db.dao

import androidx.room.*
import com.mappingsolution.data.db.entity.PoiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {

    @Query("SELECT * FROM pois ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PoiEntity>>

    @Query("SELECT * FROM pois WHERE groupId = :groupId ORDER BY createdAt ASC")
    fun observeByGroup(groupId: Long): Flow<List<PoiEntity>>

    @Query("SELECT * FROM pois WHERE groupId IS NULL ORDER BY createdAt ASC")
    fun observeOrphans(): Flow<List<PoiEntity>>

    @Query("SELECT COUNT(*) FROM pois WHERE groupId = :groupId")
    suspend fun countByGroupId(groupId: Long): Int

    @Query("SELECT * FROM pois WHERE id = :id")
    suspend fun getById(id: Long): PoiEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(poi: PoiEntity): Long

    @Update
    suspend fun update(poi: PoiEntity)

    @Delete
    suspend fun delete(poi: PoiEntity)

    @Query("DELETE FROM pois WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE pois SET groupId = NULL, updatedAt = :now WHERE id IN (:ids)")
    suspend fun orphan(ids: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE pois SET groupId = :groupId, updatedAt = :now WHERE id IN (:ids)")
    suspend fun moveToGroup(ids: List<Long>, groupId: Long, now: Long = System.currentTimeMillis())
}
