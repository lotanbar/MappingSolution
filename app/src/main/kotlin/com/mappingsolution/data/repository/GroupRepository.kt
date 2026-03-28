package com.mappingsolution.data.repository

import com.mappingsolution.data.db.dao.GroupDao
import com.mappingsolution.data.db.entity.GroupEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(private val dao: GroupDao) {

    fun observeAll(): Flow<List<GroupEntity>> = dao.observeAll()

    suspend fun getById(id: Long): GroupEntity? = dao.getById(id)

    /**
     * Validates uniqueness of all group fields before inserting/updating.
     * Returns a [DuplicateFieldError] if any collision is found, null on success.
     */
    suspend fun insert(group: GroupEntity): Result<Long> {
        checkDuplicates(group)?.let { return Result.failure(it) }
        return runCatching { dao.insert(group) }
    }

    suspend fun update(group: GroupEntity): Result<Unit> {
        checkDuplicates(group, excludeId = group.id)?.let { return Result.failure(it) }
        return runCatching { dao.update(group.copy(updatedAt = System.currentTimeMillis())) }
    }

    suspend fun delete(group: GroupEntity) = dao.delete(group)

    private suspend fun checkDuplicates(group: GroupEntity, excludeId: Long = 0): DuplicateFieldError? {
        val name = group.name.trim()
        if (dao.countByName(name, excludeId) > 0) return DuplicateFieldError.Name
        group.description?.trim()?.takeIf { it.isNotEmpty() }?.let { desc ->
            if (dao.countByDescription(desc, excludeId) > 0) return DuplicateFieldError.Description
        }
        if (dao.countByIconKey(group.iconKey, excludeId) > 0) return DuplicateFieldError.Icon
        if (dao.countByColor(group.color, excludeId) > 0) return DuplicateFieldError.Color
        return null
    }
}

sealed class DuplicateFieldError(message: String) : Exception(message) {
    object Name : DuplicateFieldError("A group with this name already exists")
    object Description : DuplicateFieldError("A group with this description already exists")
    object Icon : DuplicateFieldError("A group with this icon already exists")
    object Color : DuplicateFieldError("A group with this color already exists")
}
