package com.mappingsolution.data.places

import android.util.Log
import com.mappingsolution.data.model.Poi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OsmApiService @Inject constructor(private val httpClient: OkHttpClient) {

    private val overpassUrl = "https://overpass-api.de/api/interpreter"
    private val formMediaType = "application/x-www-form-urlencoded".toMediaType()

    /**
     * Fetches natural/historic POI nodes within the given bounding box.
     * Returns all matching nodes (no count cap — natural features are sparse).
     */
    suspend fun fetchPois(south: Double, west: Double, north: Double, east: Double): List<Poi> {
        val query = """
            [out:json][timeout:25][bbox:$south,$west,$north,$east];
            (
              node[natural~"^(peak|volcano|cave_entrance|waterfall|glacier|hot_spring|geyser)${'$'}"][name];
              node[leisure=nature_reserve][name];
              node[amenity=observatory][name];
              node[historic~"^(monument|castle|archaeological_site|ruins|fort|memorial)${'$'}"][name];
              node[tourism=viewpoint][name];
              node[man_made=lighthouse][name];
            );
            out body;
        """.trimIndent()

        val body = "data=${java.net.URLEncoder.encode(query, "UTF-8")}".toRequestBody(formMediaType)
        val request = Request.Builder()
            .url(overpassUrl)
            .post(body)
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("OsmApiService", "HTTP ${response.code}: ${response.body?.string()}")
                    return@runCatching emptyList()
                }
                val json = JSONObject(response.body!!.string())
                val elements = json.optJSONArray("elements") ?: return@runCatching emptyList()
                val now = System.currentTimeMillis()
                (0 until elements.length()).mapNotNull { i ->
                    runCatching {
                        val el = elements.getJSONObject(i)
                        val tags = el.optJSONObject("tags") ?: return@runCatching null
                        val name = tags.optString("name").takeIf { it.isNotBlank() }
                            ?: tags.optString("name:en").takeIf { it.isNotBlank() }
                            ?: return@runCatching null
                        val tagsMap = tags.keys().asSequence().associateWith { tags.getString(it) }
                        val resolvedIconKey = PoiIconResolver.resolveForOsmTags(tagsMap)
                        Poi(
                            id = "osm_${el.getLong("id")}",
                            groupId = OSM_POI_GROUP_ID,
                            name = name,
                            lat = el.getDouble("lat"),
                            lng = el.getDouble("lon"),
                            iconKey = resolvedIconKey,
                            createdAt = now,
                            updatedAt = now,
                        )
                    }.getOrNull()
                }
            }
        }.getOrElse { e ->
            Log.e("OsmApiService", "fetchPois failed", e)
            emptyList()
        }
    }
}
