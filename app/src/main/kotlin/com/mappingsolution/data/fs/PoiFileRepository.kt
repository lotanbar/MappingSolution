package com.mappingsolution.data.fs

import com.mappingsolution.data.model.Poi
import com.mappingsolution.data.util.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiFileRepository @Inject constructor(private val storageManager: StorageManager) {

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())

    init {
        CoroutineScope(Dispatchers.IO).launch { loadAll() }
    }

    private fun loadAll() {
        val dir = storageManager.getPoisDir()
        _pois.value = dir.listFiles { f -> f.isDirectory }
            ?.mapNotNull { poiDir ->
                val jsonFile = File(poiDir, "poi.json")
                if (jsonFile.exists()) readPoi(jsonFile) else null
            }
            ?.sortedBy { it.createdAt }
            ?: emptyList()
    }

    fun observeAll(): Flow<List<Poi>> = _pois
    fun observeByGroup(groupId: String): Flow<List<Poi>> = _pois.map { list -> list.filter { it.groupId == groupId } }
    fun observeOrphans(): Flow<List<Poi>> = _pois.map { list -> list.filter { it.groupId == null } }

    suspend fun countByGroup(groupId: String): Int = _pois.value.count { it.groupId == groupId }

    suspend fun getById(id: String): Poi? = _pois.value.find { it.id == id }

    suspend fun insert(poi: Poi): String = withContext(Dispatchers.IO) {
        val newPoi = poi.copy(id = if (poi.id.isEmpty()) UUID.randomUUID().toString() else poi.id)
        writePoi(newPoi)
        _pois.value = (_pois.value + newPoi).sortedBy { it.createdAt }
        newPoi.id
    }

    suspend fun update(poi: Poi) = withContext(Dispatchers.IO) {
        val old = _pois.value.find { it.id == poi.id }
        if (old != null && old.name != poi.name) {
            storageManager.renamePoiFolder(old.name, poi.name, poi.id)
        }
        val updated = poi.copy(updatedAt = System.currentTimeMillis())
        writePoi(updated)
        _pois.value = _pois.value.map { if (it.id == poi.id) updated else it }
    }

    suspend fun delete(poi: Poi) = withContext(Dispatchers.IO) {
        storageManager.deletePoiFolder(poi.name, poi.id)
        _pois.value = _pois.value.filter { it.id != poi.id }
    }

    suspend fun deleteByIds(ids: List<String>) = withContext(Dispatchers.IO) {
        val toDelete = _pois.value.filter { it.id in ids }
        toDelete.forEach { storageManager.deletePoiFolder(it.name, it.id) }
        _pois.value = _pois.value.filter { it.id !in ids }
    }

    suspend fun orphan(ids: List<String>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        _pois.value = _pois.value.map { poi ->
            if (poi.id in ids) poi.copy(groupId = null, updatedAt = now).also { writePoi(it) }
            else poi
        }
    }

    suspend fun moveToGroup(ids: List<String>, groupId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        _pois.value = _pois.value.map { poi ->
            if (poi.id in ids) poi.copy(groupId = groupId, updatedAt = now).also { writePoi(it) }
            else poi
        }
    }

    private fun writePoi(poi: Poi) {
        val json = JSONObject().apply {
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
        }
        storageManager.getPoiFile(poi.name, poi.id).writeText(json.toString())
    }

    private fun readPoi(file: File): Poi? = try {
        val json = JSONObject(file.readText())
        val mediaArr = json.optJSONArray("mediaPaths")
        val mediaPaths = if (mediaArr != null) List(mediaArr.length()) { mediaArr.getString(it) } else emptyList()
        Poi(
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
    } catch (_: Exception) { null }
}
