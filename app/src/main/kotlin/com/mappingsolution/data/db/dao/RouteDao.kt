package com.mappingsolution.data.db.dao

import androidx.room.*
import com.mappingsolution.data.db.entity.RouteEntity
import com.mappingsolution.data.db.entity.RoutePointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Query("SELECT * FROM routes ORDER BY startedAt ASC")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE didUserTapStop = 0")
    suspend fun getIncomplete(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: Long): RouteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(route: RouteEntity): Long

    @Query(
        """UPDATE routes SET name = :name, description = :description, color = :color,
           updatedAt = :updatedAt WHERE id = :id"""
    )
    suspend fun updateFields(id: Long, name: String, description: String?, color: String, updatedAt: Long)

    @Update
    suspend fun update(route: RouteEntity)

    @Delete
    suspend fun delete(route: RouteEntity)

    @Query("DELETE FROM routes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    // Route points
    @Insert
    suspend fun insertPoints(points: List<RoutePointEntity>)

    @Query("SELECT * FROM route_points WHERE routeId = :routeId ORDER BY orderIndex ASC")
    suspend fun getPoints(routeId: Long): List<RoutePointEntity>

    @Query("DELETE FROM route_points WHERE routeId = :routeId")
    suspend fun deletePoints(routeId: Long)
}
