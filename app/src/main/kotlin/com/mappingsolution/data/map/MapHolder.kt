package com.mappingsolution.data.map

import com.mappingsolution.data.prefs.ViewportPreference
import org.maplibre.android.maps.MapLibreMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide singleton that holds a reference to the active [MapLibreMap].
 *
 * [MapComponent] registers the map after the style is ready and unregisters
 * it when the composable is disposed. [RecordingService] reads the reference
 * to pass into [SmartTrackProcessor] for tile-based road snapping.
 */
@Singleton
class MapHolder @Inject constructor(
    private val viewportPreference: ViewportPreference,
) {
    @Volatile
    private var _map: MapLibreMap? = null

    /** In-memory last camera — updated by both the idle listener and on map dispose. */
    @Volatile
    private var lastCamera: ViewportPreference.SavedCamera? = null

    val map: MapLibreMap? get() = _map

    fun register(map: MapLibreMap) {
        _map = map
    }

    fun unregister() {
        _map = null
    }

    fun saveCamera(lat: Double, lng: Double, zoom: Double, bearing: Double, tilt: Double) {
        val cam = ViewportPreference.SavedCamera(lat, lng, zoom, bearing, tilt)
        lastCamera = cam
        viewportPreference.save(lat, lng, zoom, bearing, tilt)
    }

    /** Returns in-memory last position (survives navigation), falling back to disk (survives restarts). */
    fun loadCamera(): ViewportPreference.SavedCamera? = lastCamera ?: viewportPreference.load()
}
