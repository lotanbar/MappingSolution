package com.mappingsolution.ui.searchnplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.map.MapHolder
import com.mappingsolution.data.model.SearchResult
import com.mappingsolution.data.search.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
}
