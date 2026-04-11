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

@Singleton
class OsmPoiRepository @Inject constructor(
    private val api: OsmApiService,
    private val cache: OsmPoiCache,
) {

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())
    val pois: StateFlow<List<Poi>> = _pois.asStateFlow()

    fun getById(id: String): Poi? = _pois.value.find { it.id == id }

    /**
     * Fetches OSM natural/historic POIs for the given viewport bounds.
     * Cache TTL is 30 days — natural features don't move.
     */
    suspend fun refreshForViewport(
        north: Double,
        south: Double,
        east: Double,
        west: Double,
    ) = withContext(Dispatchers.IO) {
        val centerLat = (north + south) / 2.0
        val centerLng = (east + west) / 2.0
        val cacheKey = "%.2f_%.2f".format(centerLat, centerLng)

        val cached = cache.load(cacheKey)
        if (cached != null) {
            _pois.value = cached.filter { it.lat in south..north && it.lng in west..east }
            return@withContext
        }

        val fetched = runCatching {
            api.fetchPois(south, west, north, east)
        }.getOrElse { e ->
            Log.e("OsmPoiRepo", "fetch failed", e)
            emptyList()
        }

        cache.store(cacheKey, fetched)
        _pois.value = fetched.filter { it.lat in south..north && it.lng in west..east }
    }

    /** Clears in-memory POIs (e.g. when zoomed below threshold). */
    fun clear() { _pois.value = emptyList() }

    /** Called once on app launch to purge stale cache files. Does not refetch. */
    suspend fun evictStaleCacheOnLaunch() = withContext(Dispatchers.IO) {
        runCatching { cache.evictStale() }
            .onFailure { Log.w("OsmPoiRepo", "Cache eviction failed", it) }
    }
}
