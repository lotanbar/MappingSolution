package com.mappingsolution.data.fs

import com.mappingsolution.data.model.Group
import com.mappingsolution.data.places.GOOGLE_PLACES_GROUP_ID
import com.mappingsolution.data.places.OSM_POI_GROUP_ID
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
            seedPlacesGroups()
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

    /** Seeds the Google Places and OSM POI groups once, using fixed IDs. Idempotent. */
    private fun seedPlacesGroups() {
        val existingIds = _groups.value.map { it.id }.toSet()
        if (GOOGLE_PLACES_GROUP_ID !in existingIds) {
            insertRaw(
                Group(
                    id = GOOGLE_PLACES_GROUP_ID,
                    name = "Google Places",
                    description = "Nearby businesses from Google",
                    iconKey = "place",
                    color = "#FF4285F4",
                    isImported = true,
                )
            )
            android.util.Log.i("GroupFileRepository", "Seeded Google Places group")
        }
        if (OSM_POI_GROUP_ID !in existingIds) {
            insertRaw(
                Group(
                    id = OSM_POI_GROUP_ID,
                    name = "OpenStreetMap POIs",
                    description = "Natural & historic landmarks from OSM",
                    iconKey = "terrain",
                    color = "#FF4CAF50",
                    isImported = true,
                )
            )
            android.util.Log.i("GroupFileRepository", "Seeded OpenStreetMap POIs group")
        }
    }

    private fun insertRaw(group: Group) {
        writeGroup(group)
        _groups.value = (_groups.value + group).sortedBy { it.createdAt }
    }

    /**
     * For import: if an imported group with this name already exists, purges all its POIs and
     * deletes it to guarantee a clean slate. Then creates a fresh group marked as incomplete.
     * The caller must call [markImportComplete] after all data has been saved.
     */
    suspend fun purgeAndCreateForImport(
        name: String,
        poiRepository: PoiFileRepository,
        onProgress: suspend (phase: String, done: Int, total: Int) -> Unit = { _, _, _ -> },
    ): String = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        val existing = _groups.value.find { it.name.trim().equals(trimmed, ignoreCase = true) && it.isImported }
        if (existing != null) {
            if (existing.isBulk) {
                // Bulk groups: delete the whole group folder (jsonl + images) wholesale
                storageManager.deletePoiFolder(existing.name, existing.id)
            } else {
                val poiIds = poiRepository.getIdsByGroup(existing.id)
                poiRepository.deleteByIds(poiIds) { done, total ->
                    onProgress("Removing previous import…", done, total)
                }
            }
            storageManager.getGroupFile(existing.name).delete()
            _groups.value = _groups.value.filter { it.id != existing.id }
        }

        val usedIcons = _groups.value.map { it.iconKey }.toSet()
        val usedColors = _groups.value.map { it.color }.toSet()

        val candidateIcons = listOf(
            "flag", "tour", "explore", "map", "navigation", "push_pin",
            "place", "near_me", "gps_fixed", "travel_explore", "satellite",
            "location_city", "park", "terrain", "forest", "landscape",
        )
        val candidateColors = listOf(
            "#FF4CAF50", "#FFFF9800", "#FF9C27B0", "#FF00BCD4",
            "#FF607D8B", "#FFE91E63", "#FF3F51B5", "#FFFF5722",
            "#FF009688", "#FF795548", "#FF8BC34A", "#FFFFC107",
            "#FFCDDC39", "#FF00E5FF", "#FFFF4081", "#FF69F0AE",
        )

        val iconKey = candidateIcons.firstOrNull { it !in usedIcons } ?: "place"
        val color = candidateColors.firstOrNull { it !in usedColors } ?: "#FF4CAF50"

        val group = Group(name = trimmed, iconKey = iconKey, color = color, isImported = true, isBulk = true, importComplete = false)
        insertRaw(group)
        group.id
    }

    suspend fun markImportComplete(groupId: String, bulkPoiCount: Int = 0) = withContext(Dispatchers.IO) {
        val group = _groups.value.find { it.id == groupId } ?: return@withContext
        val updated = group.copy(importComplete = true, bulkPoiCount = bulkPoiCount, updatedAt = System.currentTimeMillis())
        writeGroup(updated)
        _groups.value = _groups.value.map { if (it.id == groupId) updated else it }
    }

    /**
     * @deprecated Use [purgeAndCreateForImport] instead.
     * For import: returns the ID of an existing group whose name matches (case-insensitive),
     * or creates a new one with a free icon/color combination.
     */
    suspend fun findOrCreateForImport(name: String): String = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        _groups.value.find { it.name.trim().equals(trimmed, ignoreCase = true) }
            ?.let { return@withContext it.id }

        val usedIcons = _groups.value.map { it.iconKey }.toSet()
        val usedColors = _groups.value.map { it.color }.toSet()

        val candidateIcons = listOf(
            "flag", "tour", "explore", "map", "navigation", "push_pin",
            "place", "near_me", "gps_fixed", "travel_explore", "satellite",
            "location_city", "park", "terrain", "forest", "landscape",
        )
        val candidateColors = listOf(
            "#FF4CAF50", "#FFFF9800", "#FF9C27B0", "#FF00BCD4",
            "#FF607D8B", "#FFE91E63", "#FF3F51B5", "#FFFF5722",
            "#FF009688", "#FF795548", "#FF8BC34A", "#FFFFC107",
            "#FFCDDC39", "#FF00E5FF", "#FFFF4081", "#FF69F0AE",
        )

        val iconKey = candidateIcons.firstOrNull { it !in usedIcons } ?: "place"
        val color = candidateColors.firstOrNull { it !in usedColors } ?: "#FF4CAF50"

        val group = Group(name = trimmed, iconKey = iconKey, color = color, isImported = true)
        insertRaw(group)
        group.id
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

    /** Toggles the isVisible flag without running duplicate validation. Safe to call for any group. */
    suspend fun setVisibility(groupId: String, isVisible: Boolean) = withContext(Dispatchers.IO) {
        val group = _groups.value.find { it.id == groupId } ?: return@withContext
        val updated = group.copy(isVisible = isVisible, updatedAt = System.currentTimeMillis())
        writeGroup(updated)
        _groups.value = _groups.value.map { if (it.id == groupId) updated else it }
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
            put("isImported", group.isImported)
            put("importComplete", group.importComplete)
            put("isBulk", group.isBulk)
            put("bulkPoiCount", group.bulkPoiCount)
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
            isImported = json.optBoolean("isImported", false),
            importComplete = json.optBoolean("importComplete", true),
            isBulk = json.optBoolean("isBulk", false),
            bulkPoiCount = json.optInt("bulkPoiCount", 0),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )
    } catch (_: Exception) { null }
}
