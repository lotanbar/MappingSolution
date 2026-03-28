package com.mappingsolution.data.db.dao

import androidx.room.*
import com.mappingsolution.data.db.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM groups ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: Long): GroupEntity?

    @Query("SELECT COUNT(*) FROM groups WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long = 0): Int

    @Query("SELECT COUNT(*) FROM groups WHERE description = :desc AND id != :excludeId")
    suspend fun countByDescription(desc: String, excludeId: Long = 0): Int

    @Query("SELECT COUNT(*) FROM groups WHERE iconKey = :iconKey AND id != :excludeId")
    suspend fun countByIconKey(iconKey: String, excludeId: Long = 0): Int

    @Query("SELECT COUNT(*) FROM groups WHERE color = :color AND id != :excludeId")
    suspend fun countByColor(color: String, excludeId: Long = 0): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(group: GroupEntity): Long

    @Update
    suspend fun update(group: GroupEntity)

    @Delete
    suspend fun delete(group: GroupEntity)

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun count(): Int
}
