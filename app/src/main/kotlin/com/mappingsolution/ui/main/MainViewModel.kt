package com.mappingsolution.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.fs.GroupFileRepository
import com.mappingsolution.data.fs.PoiFileRepository
import com.mappingsolution.data.fs.RouteFileRepository
import com.mappingsolution.data.map.MapHolder
import com.mappingsolution.data.model.Group
import com.mappingsolution.data.model.Poi
import com.mappingsolution.data.model.Route
import com.mappingsolution.data.model.RoutePoint
import com.mappingsolution.data.prefs.ViewportPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    groupRepository: GroupFileRepository,
    poiRepository: PoiFileRepository,
    private val routeRepository: RouteFileRepository,
    val mapHolder: MapHolder,
) : ViewModel() {

    val groups: StateFlow<List<Group>> = groupRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pois: StateFlow<List<Poi>> = poiRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routes: StateFlow<List<Route>> = routeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Routes that were not properly stopped (app killed / battery died during recording). */
    val incompleteRoutes: StateFlow<List<Route>> = routeRepository.observeAll()
        .map { routes -> routes.filter { !it.didUserTapStop } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Points for all visible completed routes, keyed by route ID. Used to render polylines on the map. */
    val routePoints: StateFlow<Map<String, List<RoutePoint>>> = routeRepository.observeAll()
        .flatMapLatest { routes ->
            flow {
                val result = routes
                    .filter { it.isVisible && it.didUserTapStop }
                    .associate { route -> route.id to routeRepository.getPoints(route.id) }
                emit(result)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Reads last known viewport — always fresh (in-memory first, then disk). */
    val initialCamera: ViewportPreference.SavedCamera? get() = mapHolder.loadCamera()

    fun saveCameraPosition(lat: Double, lng: Double, zoom: Double, bearing: Double, tilt: Double) {
        mapHolder.saveCamera(lat, lng, zoom, bearing, tilt)
    }
}
