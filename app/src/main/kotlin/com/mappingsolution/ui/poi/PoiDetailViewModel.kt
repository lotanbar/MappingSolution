package com.mappingsolution.ui.poi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.db.entity.GroupEntity
import com.mappingsolution.data.db.entity.PoiEntity
import com.mappingsolution.data.repository.GroupRepository
import com.mappingsolution.data.repository.PoiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

data class PoiDetailState(
    val poi: PoiEntity? = null,
    val group: GroupEntity? = null,
    val mediaPaths: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PoiDetailViewModel @Inject constructor(
    private val poiRepository: PoiRepository,
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val poiId: Long = requireNotNull(savedStateHandle.get<Long>("poiId"))

    private val _state = MutableStateFlow(PoiDetailState())
    val state: StateFlow<PoiDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val poi = poiRepository.getById(poiId) ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val group = poi.groupId?.let { groupRepository.getById(it) }
            val mediaPaths = try {
                val arr = JSONArray(poi.mediaPaths)
                List(arr.length()) { arr.getString(it) }
            } catch (_: Exception) { emptyList() }
            _state.update {
                PoiDetailState(poi = poi, group = group, mediaPaths = mediaPaths, isLoading = false)
            }
        }
    }
}
