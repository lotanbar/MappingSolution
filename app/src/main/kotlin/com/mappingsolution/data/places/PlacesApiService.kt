package com.mappingsolution.data.places

import android.util.Log
import com.mappingsolution.BuildConfig
import com.mappingsolution.data.model.Poi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlacesApiService @Inject constructor(private val httpClient: OkHttpClient) {

    private val baseUrl = "https://places.googleapis.com/v1/places:searchNearby"
    private val detailBaseUrl = "https://places.googleapis.com/v1/places"
    private val v1BaseUrl = "https://places.googleapis.com/v1"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Fetches nearby places centered on [lat]/[lng] within [radiusMeters].
     * Returns at most [maxCount] results (capped by the API at 20).
     */
    suspend fun fetchNearby(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        maxCount: Int,
    ): List<Poi> {
        val apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
        if (apiKey.isBlank()) {
            Log.w("PlacesApiService", "GOOGLE_PLACES_API_KEY is not set; skipping fetch")
            return emptyList()
        }
        val effectiveMax = maxCount
        val body = JSONObject().apply {
            put("includedTypes", org.json.JSONArray(GOOGLE_PLACES_INCLUDED_TYPES))
            put("maxResultCount", effectiveMax)
            put("locationRestriction", JSONObject().apply {
                put("circle", JSONObject().apply {
                    put("center", JSONObject().apply {
                        put("latitude", lat)
                        put("longitude", lng)
                    })
                    put("radius", radiusMeters)
                })
            })
        }.toString()

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", GOOGLE_PLACES_FIELD_MASK)
            .post(body.toRequestBody(jsonMediaType))
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("PlacesApiService", "HTTP ${response.code}: ${response.body?.string()}")
                    return@runCatching emptyList()
                }
                val json = JSONObject(response.body!!.string())
                val placesArray = json.optJSONArray("places") ?: return@runCatching emptyList()
                val now = System.currentTimeMillis()
                (0 until placesArray.length()).mapNotNull { i ->
                    runCatching {
                        val place = placesArray.getJSONObject(i)
                        val location = place.getJSONObject("location")
                        val name = place.getJSONObject("displayName").getString("text")
                        val typesArray = place.optJSONArray("types")
                        val types = if (typesArray != null) {
                            (0 until typesArray.length()).map { typesArray.getString(it) }
                        } else emptyList()
                        val resolvedIconKey = PoiIconResolver.resolveForGoogleType(types)
                        Poi(
                            id = place.getString("id"),
                            groupId = GOOGLE_PLACES_GROUP_ID,
                            name = name,
                            lat = location.getDouble("latitude"),
                            lng = location.getDouble("longitude"),
                            iconKey = resolvedIconKey,
                            createdAt = now,
                            updatedAt = now,
                        )
                    }.getOrNull()
                }
            }
        }.getOrElse { e ->
            Log.e("PlacesApiService", "fetchNearby failed", e)
            emptyList()
        }
    }

    /**
     * Fetches up to 3 photo URLs for a specific place by ID.
     * Photo URL format: https://places.googleapis.com/v1/{photoName}/media?maxWidthPx=800&key={key}
     */
    suspend fun fetchPlacePhotoUrls(placeId: String): List<String> {
        val apiKey = BuildConfig.GOOGLE_PLACES_API_KEY
        if (apiKey.isBlank()) return emptyList()

        val request = Request.Builder()
            .url("$detailBaseUrl/$placeId")
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", "photos")
            .get()
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("PlacesApiService", "fetchPlacePhotoUrls HTTP ${response.code}")
                    return@runCatching emptyList()
                }
                val json = JSONObject(response.body!!.string())
                val photos = json.optJSONArray("photos") ?: return@runCatching emptyList()
                (0 until minOf(photos.length(), 3)).mapNotNull { i ->
                    runCatching {
                        val photoName = photos.getJSONObject(i).getString("name")
                        // photoName = "places/{id}/photos/{ref}" — base is v1, not v1/places
                        "$v1BaseUrl/$photoName/media?maxWidthPx=800&key=$apiKey"
                    }.getOrNull()
                }
            }
        }.getOrElse { e ->
            Log.e("PlacesApiService", "fetchPlacePhotoUrls failed", e)
            emptyList()
        }
    }
}
