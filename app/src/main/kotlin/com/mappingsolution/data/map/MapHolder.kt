package com.mappingsolution.data.map

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
class MapHolder @Inject constructor() {

    @Volatile
    private var _map: MapLibreMap? = null

    val map: MapLibreMap? get() = _map

    fun register(map: MapLibreMap) {
        _map = map
    }

    fun unregister() {
        _map = null
    }
}
