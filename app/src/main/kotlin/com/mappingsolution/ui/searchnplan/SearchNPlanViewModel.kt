package com.mappingsolution.ui.searchnplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.map.MapHolder
import com.mappingsolution.data.model.DestinationSource
import com.mappingsolution.data.model.PlanDestination
import com.mappingsolution.data.model.SearchResult
import com.mappingsolution.data.search.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchNPlanViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val mapHolder: MapHolder,
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    val isLoading = MutableStateFlow(false)

    private val _destinations = MutableStateFlow<List<PlanDestination>>(emptyList())
    val destinations: StateFlow<List<PlanDestination>> = _destinations.asStateFlow()

    private val _activeRowIndex = MutableStateFlow<Int?>(null)
    val activeRowIndex: StateFlow<Int?> = _activeRowIndex.asStateFlow()

    init {
        viewModelScope.launch {
            searchQuery.collectLatest { query ->
                if (query.length < 3) {
                    _results.value = emptyList()
                    isLoading.value = false
                    return@collectLatest
                }
                delay(300)
                val camera = mapHolder.loadCamera()
                val lat = camera?.lat ?: 0.0
                val lng = camera?.lng ?: 0.0
                isLoading.value = true
                try {
                    _results.value = searchRepository.search(
                        query = query,
                        userLat = lat,
                        userLng = lng,
                        viewSouth = lat - 0.5,
                        viewWest = lng - 0.5,
                        viewNorth = lat + 0.5,
                        viewEast = lng + 0.5,
                        skipRemote = camera == null,
                    )
                } finally {
                    isLoading.value = false
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        searchQuery.value = query
    }

    fun activateRow(index: Int) {
        _activeRowIndex.value = index
        searchQuery.value = ""
        _results.value = emptyList()
        isLoading.value = false
    }

    fun deactivateRow() {
        _activeRowIndex.value = null
        searchQuery.value = ""
        _results.value = emptyList()
        isLoading.value = false
    }

    fun addDestination(result: SearchResult) {
        val activeIdx = _activeRowIndex.value ?: return
        val source = when (result) {
            is SearchResult.PersonalPoi -> DestinationSource.PERSONAL
            is SearchResult.ImportedPoi -> DestinationSource.IMPORTED
            is SearchResult.OsmPoi -> DestinationSource.OSM
            is SearchResult.GooglePlace -> DestinationSource.GOOGLE
        }
        val dest = PlanDestination(
            sourceType = source,
            sourceId = result.poi.id,
            name = result.poi.name,
            lat = result.poi.lat,
            lng = result.poi.lng,
        )
        _destinations.update { list ->
            if (activeIdx >= list.size) {
                list + dest
            } else {
                list.toMutableList().apply { set(activeIdx, dest) }
            }
        }
        deactivateRow()
    }

    fun removeDestination(id: String) {
        _destinations.update { list -> list.filter { it.id != id } }
    }

    fun moveDestination(from: Int, to: Int) {
        _destinations.update { list ->
            if (from !in list.indices || to !in list.indices) return@update list
            list.toMutableList().apply { add(to, removeAt(from)) }
        }
    }
}
