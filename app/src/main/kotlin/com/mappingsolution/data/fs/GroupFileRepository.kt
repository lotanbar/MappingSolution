package com.mappingsolution.data.fs

import com.mappingsolution.data.model.Group
import com.mappingsolution.data.util.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class DuplicateFieldError(message: String) : Exception(message) {
    object Name        : DuplicateFieldError("A group with this name already exists")
    object Description : DuplicateFieldError("A group with this description already exists")
    object Icon        : DuplicateFieldError("A group with this icon already exists")
    object Color       : DuplicateFieldError("A group with this color already exists")
}

@Singleton
class GroupFileRepository @Inject constructor(private val storageManager: StorageManager) {

    private val _groups = MutableStateFlow<List<Group>>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            loadAll()
            if (_groups.value.isEmpty()) seedDefault()
        }
    }

    private fun loadAll() {
        val files = storageManager.getGroupsDir().listFiles { f -> f.extension == "json" } ?: return
        _groups.value = files.mapNotNull { readGroup(it) }.sortedBy { it.createdAt }
    }

    private suspend fun seedDefault() {
        val default = Group(
            name = "Personal POIs",
            description = "My personal points of interest",
            iconKey = "place",
            color = "#FF2196F3",
        )
        insertRaw(default)
    }

    private fun insertRaw(group: Group) {
        writeGroup(group)
        _groups.value = (_groups.value + group).sortedBy { it.createdAt }
    }

    fun observeAll(): Flow<List<Group>> = _groups

    suspend fun getById(id: String): Group? = _groups.value.find { it.id == id }

    suspend fun insert(group: Group): Result<String> = withContext(Dispatchers.IO) {
        checkDuplicates(group)?.let { return@withContext Result.failure(it) }
        val newGroup = group.copy(id = if (group.id.isEmpty()) UUID.randomUUID().toString() else group.id)
        writeGroup(newGroup)
        _groups.value = (_groups.value + newGroup).sortedBy { it.createdAt }
        Result.success(newGroup.id)
    }

    suspend fun update(group: Group): Result<Unit> = withContext(Dispatchers.IO) {
        checkDuplicates(group, excludeId = group.id)?.let { return@withContext Result.failure(it) }
        val old = _groups.value.find { it.id == group.id }
        // If name changed, delete the old file (new name = new filename)
        if (old != null && old.name != group.name) {
            storageManager.getGroupFile(old.name).delete()
        }
        val updated = group.copy(updatedAt = System.currentTimeMillis())
        writeGroup(updated)
        _groups.value = _groups.value.map { if (it.id == group.id) updated else it }
        Result.success(Unit)
    }

    suspend fun delete(group: Group) = withContext(Dispatchers.IO) {
        storageManager.getGroupFile(group.name).delete()
        _groups.value = _groups.value.filter { it.id != group.id }
    }

    private fun checkDuplicates(group: Group, excludeId: String = ""): DuplicateFieldError? {
        val others = _groups.value.filter { it.id != excludeId }
        val name = group.name.trim()
        if (others.any { it.name.trim().equals(name, ignoreCase = true) }) return DuplicateFieldError.Name
        group.description?.trim()?.takeIf { it.isNotEmpty() }?.let { desc ->
            if (others.any { it.description?.trim().equals(desc, ignoreCase = true) }) return DuplicateFieldError.Description
        }
        if (others.any { it.iconKey == group.iconKey }) return DuplicateFieldError.Icon
        if (others.any { it.color == group.color }) return DuplicateFieldError.Color
        return null
    }

    private fun writeGroup(group: Group) {
        val json = JSONObject().apply {
            put("id", group.id)
            put("name", group.name)
            group.description?.let { put("description", it) }
            put("iconKey", group.iconKey)
            put("color", group.color)
            put("isVisible", group.isVisible)
            put("createdAt", group.createdAt)
            put("updatedAt", group.updatedAt)
        }
        storageManager.getGroupFile(group.name).writeText(json.toString())
    }

    private fun readGroup(file: File): Group? = try {
        val json = JSONObject(file.readText())
        Group(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.optString("description").takeIf { it.isNotEmpty() },
            iconKey = json.getString("iconKey"),
            color = json.getString("color"),
            isVisible = json.optBoolean("isVisible", true),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )
    } catch (_: Exception) { null }
}
