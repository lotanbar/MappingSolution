package com.mappingsolution.data.db.dao

import androidx.room.*
import com.mappingsolution.data.db.entity.RouteEntity
import com.mappingsolution.data.db.entity.RoutePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes ORDER BY startedAt ASC")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE groupId IS NULL ORDER BY startedAt ASC")
    fun observeOrphans(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE didUserTapStop = 0")
    suspend fun getIncomplete(): List<RouteEntity>

    @Query("SELECT COUNT(*) FROM routes WHERE groupId = :groupId")
    suspend fun countByGroupId(groupId: Long): Int

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: Long): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(route: RouteEntity): Long

    @Update
    suspend fun update(route: RouteEntity)

    @Delete
    suspend fun delete(route: RouteEntity)

    @Query("DELETE FROM routes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE routes SET groupId = NULL, updatedAt = :now WHERE id IN (:ids)")
    suspend fun orphan(ids: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE routes SET groupId = :groupId, updatedAt = :now WHERE id IN (:ids)")
    suspend fun moveToGroup(ids: List<Long>, groupId: Long, now: Long = System.currentTimeMillis())

    // Route points
    @Insert
    suspend fun insertPoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY orderIndex ASC")
    suspend fun getPoints(routeId: Long): List<RoutePointEntity>

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun deletePoints(routeId: Long)
}
