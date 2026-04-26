package com.mappingsolution.data.map

import com.mappingsolution.data.prefs.HillshadePreference
import com.mappingsolution.data.prefs.MapStylePreference
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide singleton that owns the reactive map layer state:
 * - [mapStyle]: which base map is active (satellite vs. vector)
 * - [hillshadeVisible]: whether the hillshade overlay is shown
 *
 * Both [com.mappingsolution.ui.main.MainViewModel] and
 * [com.mappingsolution.ui.library.LibraryViewModel] inject this so changes
 * made in the Library propagate instantly to the map.
 */
@Singleton
class MapLayersState @Inject constructor(
    private val mapStylePreference: MapStylePreference,
    private val hillshadePreference: HillshadePreference,
) {
    val mapStyle: MutableStateFlow<MapStyle> = MutableStateFlow(mapStylePreference.load())
    val hillshadeVisible: MutableStateFlow<Boolean> = MutableStateFlow(hillshadePreference.load())

    fun setMapStyle(style: MapStyle) {
        mapStyle.value = style
        mapStylePreference.save(style)
    }

    fun setHillshadeVisible(visible: Boolean) {
        hillshadeVisible.value = visible
        hillshadePreference.save(visible)
    }
}
