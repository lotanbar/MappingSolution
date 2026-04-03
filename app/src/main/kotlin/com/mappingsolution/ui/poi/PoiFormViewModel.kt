package com.mappingsolution.ui.poi

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.db.entity.GroupEntity
import com.mappingsolution.data.db.entity.PoiEntity
import com.mappingsolution.data.repository.GroupRepository
import com.mappingsolution.data.repository.PoiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import javax.inject.Inject

data class PoiFormState(
    val name: String = "",
    val description: String = "",
    val groupId: Long? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val mediaPaths: List<String> = emptyList(),
    val nameError: String? = null,
    val mediaError: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
)

@HiltViewModel
class PoiFormViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val poiRepository: PoiRepository,
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _poiId: Long? = savedStateHandle.get<Long>("poiId")?.takeIf { it > 0 }
    val poiId: Long? get() = _poiId
    val isEditing: Boolean get() = _poiId != null

    val groups: StateFlow<List<GroupEntity>> = groupRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(
        PoiFormState(
            isLoading = poiId != null,
            lat = savedStateHandle.get<String>("lat")?.toDoubleOrNull() ?: 0.0,
            lng = savedStateHandle.get<String>("lng")?.toDoubleOrNull() ?: 0.0,
        )
    )
    val state: StateFlow<PoiFormState> = _state.asStateFlow()

    init {
        _poiId?.let { id ->
            viewModelScope.launch {
                val poi = poiRepository.getById(id)
                if (poi != null) {
                    _state.update {
                        it.copy(
                            name = poi.name,
                            description = poi.description ?: "",
                            groupId = poi.groupId,
                            lat = poi.lat,
                            lng = poi.lng,
                            mediaPaths = parseMediaPaths(poi.mediaPaths),
                            isLoading = false,
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value) }
    fun onGroupChange(groupId: Long?) = _state.update { it.copy(groupId = groupId) }

    fun addMediaUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val added = mutableListOf<String>()
            var error: String? = null
            for (uri in uris) {
                try {
                    val ext = appContext.contentResolver.getType(uri)
                        ?.substringAfterLast('/')
                        ?.let { ".$it" } ?: ""
                    val dest = File(appContext.filesDir, "poi_media_${System.currentTimeMillis()}$ext")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    added.add(dest.absolutePath)
                } catch (_: Exception) {
                    error = "Failed to attach one or more files"
                }
            }
            _state.update { it.copy(mediaPaths = it.mediaPaths + added, mediaError = error) }
        }
    }

    fun removeMediaPath(path: String) {
        _state.update { it.copy(mediaPaths = it.mediaPaths - path) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(path).delete() }
        }
    }

    fun save(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = "Name is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val mediaJson = toMediaPathsJson(s.mediaPaths)
                if (_poiId == null) {
                    poiRepository.insert(
                        PoiEntity(
                            name = s.name.trim(),
                            description = s.description.trim().ifEmpty { null },
                            groupId = s.groupId,
                            lat = s.lat,
                            lng = s.lng,
                            mediaPaths = mediaJson,
                        )
                    )
                } else {
                    val existing = poiRepository.getById(_poiId) ?: return@launch
                    poiRepository.update(
                        existing.copy(
                            name = s.name.trim(),
                            description = s.description.trim().ifEmpty { null },
                            groupId = s.groupId,
                            mediaPaths = mediaJson,
                        )
                    )
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun parseMediaPaths(json: String): List<String> = try {
        val arr = JSONArray(json)
        List(arr.length()) { arr.getString(it) }
    } catch (_: Exception) { emptyList() }

    private fun toMediaPathsJson(paths: List<String>): String {
        val arr = JSONArray()
        paths.forEach { arr.put(it) }
        return arr.toString()
    }
}
