package com.mappingsolution.data.places

import android.util.Log
import com.mappingsolution.data.model.Poi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sqrt

@Singleton
class GooglePlacesRepository @Inject constructor(
    private val api: PlacesApiService,
    private val cache: GooglePlacesCache,
) {

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())
    val pois: StateFlow<List<Poi>> = _pois.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun getById(id: String): Poi? = _pois.value.find { it.id == id }

    /** Fetches photo URLs for a specific Google Place (up to 3). Empty list on any error. */
    suspend fun fetchPhotoUrls(placeId: String): List<String> =
        withContext(Dispatchers.IO) { api.fetchPlacePhotoUrls(placeId) }

    /**
     * Fetches Google Places POIs for the given viewport bounds.
     * Uses the disk cache; only fetches from the API if the cache doesn't have enough results.
     */
    suspend fun refreshForViewport(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
    ) = withContext(Dispatchers.IO) {
        try {
        _isLoading.value = true
        val centerLat = (north + south) / 2.0
        val centerLng = (east + west) / 2.0
        val cacheKey = "%.2f_%.2f".format(centerLat, centerLng)

        val cached = cache.load(cacheKey)
        val cachedInView = cached
            ?.filter { it.lat in south..north && it.lng in west..east }
            ?: emptyList()

        val remaining = maxOf(0, GOOGLE_PLACES_MAX_RESULTS - cachedInView.size)

        if (remaining == 0) {
            _pois.value = cachedInView
            return@withContext
        }

        // Round up to even so we split evenly between N and S halves
        val remainingRounded = if (remaining % 2 != 0) remaining + 1 else remaining
        val maxPerHalf = min(remainingRounded / 2, GOOGLE_PLACES_MAX_PER_CALL)

        // Radius covers each N/S half of the viewport
        val halfLatDeg = (north - south) / 4.0
        val halfLngDeg = (east - west) / 2.0
        val radiusMeters = sqrt(
            (halfLatDeg * 111_000).let { it * it } +
            (halfLngDeg * 111_000 * cos(Math.toRadians(centerLat))).let { it * it }
        )

        val nCenterLat = (centerLat + north) / 2.0
        val sCenterLat = (centerLat + south) / 2.0

        val northPois = runCatching {
            api.fetchNearby(nCenterLat, centerLng, radiusMeters, maxPerHalf)
        }.getOrElse { e -> Log.e("GooglePlacesRepo", "N-half fetch failed", e); emptyList() }

        val southPois = runCatching {
            api.fetchNearby(sCenterLat, centerLng, radiusMeters, maxPerHalf)
        }.getOrElse { e -> Log.e("GooglePlacesRepo", "S-half fetch failed", e); emptyList() }

        val fetchedById = (northPois + southPois).associateBy { it.id }
        val cachedById = (cached ?: emptyList()).associateBy { it.id }
        val combined = (cachedById + fetchedById).values.toList()

        cache.store(cacheKey, combined)
        _pois.value = combined.filter { it.lat in south..north && it.lng in west..east }
        } finally {
            _isLoading.value = false
        }
    }

    /** Clears in-memory POIs (e.g. when zoomed below threshold). */
    fun clear() { _pois.value = emptyList() }

    /** Called once on app launch to purge stale cache files. Does not refetch. */
    suspend fun evictStaleCacheOnLaunch() = withContext(Dispatchers.IO) {
        runCatching { cache.evictStale() }
            .onFailure { Log.w("GooglePlacesRepo", "Cache eviction failed", it) }
    }
}
