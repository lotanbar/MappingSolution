package com.mappingsolution.ui.poi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.fs.GroupFileRepository
import com.mappingsolution.data.fs.PoiFileRepository
import com.mappingsolution.data.model.Group
import com.mappingsolution.data.model.Poi
import com.mappingsolution.data.util.StorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class PoiDetailState(
    val poi: Poi? = null,
    val group: Group? = null,
    val mediaPaths: List<String> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PoiDetailViewModel @Inject constructor(
    private val poiRepository: PoiFileRepository,
    private val groupRepository: GroupFileRepository,
    private val storageManager: StorageManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val poiId: String = requireNotNull(savedStateHandle.get<String>("poiId"))

    private val _state = MutableStateFlow(PoiDetailState())
    val state: StateFlow<PoiDetailState> = _state.asStateFlow()

    init { loadPoi() }

    private fun loadPoi() {
        viewModelScope.launch {
            val poi = poiRepository.getById(poiId) ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val group = poi.groupId?.let { groupRepository.getById(it) }
            val absolutePaths = poi.mediaPaths.map { filename ->
                storageManager.getPoiMediaDir(poi.name, poi.id).absolutePath + "/" + filename
            }
            _state.update { PoiDetailState(poi = poi, group = group, mediaPaths = absolutePaths, isLoading = false) }
        }
    }

    fun deletePoi(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.value.poi?.let {
                poiRepository.delete(it)
                onDeleted()
            }
        }
    }

    fun removeMediaItem(index: Int) {
        viewModelScope.launch {
            val currentState = _state.value
            val newAbsolutePaths = currentState.mediaPaths.toMutableList()
            if (index in newAbsolutePaths.indices) {
                val pathToRemove = newAbsolutePaths.removeAt(index)
                runCatching { File(pathToRemove).delete() }
                currentState.poi?.let { poi ->
                    val newFilenames = newAbsolutePaths.map { File(it).name }
                    poiRepository.update(poi.copy(mediaPaths = newFilenames))
                }
                _state.update { it.copy(mediaPaths = newAbsolutePaths) }
            }
        }
    }
}
