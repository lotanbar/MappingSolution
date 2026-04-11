package com.mappingsolution.data.places

import android.content.Context
import android.util.Log
import com.mappingsolution.data.model.Poi
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlacesCache @Inject constructor(@ApplicationContext context: Context) {

    private val cacheDir = File(context.cacheDir, "gp_poi_cache").also { it.mkdirs() }

    private fun cacheFile(key: String) = File(cacheDir, "gp_$key.json")

    /** Loads cached POIs for [key] if they exist and are younger than [GOOGLE_PLACES_CACHE_TTL_MS]. */
    fun load(key: String): List<Poi>? {
        val file = cacheFile(key)
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            val fetchedAt = json.getLong("fetchedAt")
            if (System.currentTimeMillis() - fetchedAt > GOOGLE_PLACES_CACHE_TTL_MS) return null
            val arr = json.getJSONArray("pois")
            (0 until arr.length()).mapNotNull { i ->
                runCatching { poisFromJson(arr.getJSONObject(i), fetchedAt) }.getOrNull()
            }
        }.getOrElse {
            Log.w("GooglePlacesCache", "Failed to read cache for $key", it)
            null
        }
    }

    /** Persists [pois] to disk with the current timestamp. */
    fun store(key: String, pois: List<Poi>) {
        runCatching {
            val arr = JSONArray()
            pois.forEach { arr.put(poiToJson(it)) }
            val json = JSONObject().apply {
                put("fetchedAt", System.currentTimeMillis())
                put("pois", arr)
            }
            cacheFile(key).writeText(json.toString())
        }.onFailure { Log.w("GooglePlacesCache", "Failed to write cache for $key", it) }
    }

    /** Deletes all cache files older than [GOOGLE_PLACES_CACHE_TTL_MS]. */
    fun evictStale() {
        val now = System.currentTimeMillis()
        cacheDir.listFiles { f -> f.name.startsWith("gp_") && f.extension == "json" }
            ?.forEach { file ->
                runCatching {
                    val fetchedAt = JSONObject(file.readText()).getLong("fetchedAt")
                    if (now - fetchedAt > GOOGLE_PLACES_CACHE_TTL_MS) file.delete()
                }
            }
    }

    private fun poiToJson(poi: Poi) = JSONObject().apply {
        put("id", poi.id)
        put("name", poi.name)
        put("lat", poi.lat)
        put("lng", poi.lng)
    }

    private fun poisFromJson(obj: JSONObject, fetchedAt: Long) = Poi(
        id = obj.getString("id"),
        groupId = GOOGLE_PLACES_GROUP_ID,
        name = obj.getString("name"),
        lat = obj.getDouble("lat"),
        lng = obj.getDouble("lng"),
        createdAt = fetchedAt,
        updatedAt = fetchedAt,
    )
}
