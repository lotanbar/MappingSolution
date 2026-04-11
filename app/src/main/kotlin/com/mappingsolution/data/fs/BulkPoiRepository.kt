package com.mappingsolution.data.fs

import com.mappingsolution.data.model.Group
import com.mappingsolution.data.model.Poi
import com.mappingsolution.data.util.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BulkPoiRepository @Inject constructor(private val storageManager: StorageManager) {

    private val _poisInViewport = MutableStateFlow<List<Poi>>(emptyList())
    val poisInViewport: StateFlow<List<Poi>> = _poisInViewport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun clear() { _poisInViewport.value = emptyList() }

    fun getById(id: String): Poi? = _poisInViewport.value.find { it.id == id }

    /**
     * Scans the JSONL files for all visible bulk groups and emits only the POIs that fall within
     * the given bounding box. Sequential line-by-line read — far faster than 90K file opens.
     */
    suspend fun refreshForViewport(
        bulkGroups: List<Group>,
        north: Double,
        south: Double,
        east: Double,
        west: Double,
    ) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val visibleGroups = bulkGroups.filter { it.isVisible }
            val result = mutableListOf<Poi>()
            for (group in visibleGroups) {
                val jsonlFile = storageManager.getBulkPoisFile(group.name, group.id)
                if (!jsonlFile.exists()) continue
                jsonlFile.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (line.isBlank()) return@forEach
                        try {
                            val poi = parseLine(line)
                            if (poi.lat in south..north && poi.lng in west..east) {
                                result.add(poi)
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
            _poisInViewport.value = result
        } finally {
            _isLoading.value = false
        }
    }

    /** Deletes the bulk group folder (jsonl + images) from disk. */
    fun deleteGroup(name: String, id: String) {
        storageManager.deletePoiFolder(name, id)
        _poisInViewport.value = _poisInViewport.value.filter { it.groupId != id }
    }

    private fun parseLine(line: String): Poi {
        val json = JSONObject(line)
        val mediaArr = json.optJSONArray("mediaPaths")
        val mediaPaths = if (mediaArr != null) List(mediaArr.length()) { mediaArr.getString(it) } else emptyList()
        return Poi(
            id = json.getString("id"),
            groupId = json.optString("groupId").takeIf { it.isNotEmpty() },
            name = json.getString("name"),
            description = json.optString("description").takeIf { it.isNotEmpty() },
            lat = json.getDouble("lat"),
            lng = json.getDouble("lng"),
            elevation = if (json.has("elevation")) json.getDouble("elevation") else null,
            mediaPaths = mediaPaths,
            isVisible = json.optBoolean("isVisible", true),
            createdAt = json.getLong("createdAt"),
            updatedAt = json.getLong("updatedAt"),
        )
    }

    companion object {
        /** Serializes a Poi to a single-line JSON string for storage in bulk_pois.jsonl. */
        fun serializePoi(poi: Poi): String = JSONObject().apply {
            put("id", poi.id)
            poi.groupId?.let { put("groupId", it) }
            put("name", poi.name)
            poi.description?.let { put("description", it) }
            put("lat", poi.lat)
            put("lng", poi.lng)
            poi.elevation?.let { put("elevation", it) }
            put("mediaPaths", JSONArray(poi.mediaPaths))
            put("isVisible", poi.isVisible)
            put("createdAt", poi.createdAt)
            put("updatedAt", poi.updatedAt)
        }.toString()
    }
}
