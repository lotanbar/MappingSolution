package com.mappingsolution.ui.poi

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.fs.GroupFileRepository
import com.mappingsolution.data.fs.PoiFileRepository
import com.mappingsolution.data.model.Group
import com.mappingsolution.data.model.GroupType
import com.mappingsolution.data.model.Poi
import com.mappingsolution.data.util.StorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class PoiFormState(
    val name: String = "",
    val description: String = "",
    val groupId: String? = null,
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
    private val poiRepository: PoiFileRepository,
    private val groupRepository: GroupFileRepository,
    private val storageManager: StorageManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _poiId: String? = savedStateHandle.get<String>("poiId")?.takeIf { it.isNotEmpty() }
    val poiId: String? get() = _poiId
    val isEditing: Boolean get() = _poiId != null

    val groups: StateFlow<List<Group>> = groupRepository.observeAll()
        .map { groups -> groups.filter { it.type == GroupType.POI } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(
        PoiFormState(
            isLoading = _poiId != null,
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
                    val absolutePaths = poi.mediaPaths.map { filename ->
                        storageManager.getPoiMediaDir(poi.name, poi.id).absolutePath + "/" + filename
                    }
                    _state.update {
                        it.copy(
                            name = poi.name,
                            description = poi.description ?: "",
                            groupId = poi.groupId,
                            lat = poi.lat,
                            lng = poi.lng,
                            mediaPaths = absolutePaths,
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
    fun onGroupChange(groupId: String?) = _state.update { it.copy(groupId = groupId) }

    fun addMediaUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            val added = mutableListOf<String>()
            var error: String? = null
            val tempDir = storageManager.getTempDir()
            for (uri in uris) {
                try {
                    val ext = appContext.contentResolver.getType(uri)
                        ?.substringAfterLast('/')?.let { ".$it" } ?: ""
                    val dest = File(tempDir, "temp_media_${System.currentTimeMillis()}${System.nanoTime()}$ext")
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
        viewModelScope.launch(Dispatchers.IO) { runCatching { File(path).delete() } }
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
                if (_poiId == null) {
                    val newPoi = Poi(
                        name = s.name.trim(),
                        description = s.description.trim().ifEmpty { null },
                        groupId = s.groupId,
                        lat = s.lat,
                        lng = s.lng,
                    )
                    val id = poiRepository.insert(newPoi)
                    val finalFilenames = finalizeMediaFiles(s.mediaPaths, id)
                    poiRepository.update(newPoi.copy(id = id, mediaPaths = finalFilenames))
                } else {
                    val existing = poiRepository.getById(_poiId) ?: return@launch
                    val finalFilenames = finalizeMediaFiles(s.mediaPaths, _poiId)
                    poiRepository.update(
                        existing.copy(
                            name = s.name.trim(),
                            description = s.description.trim().ifEmpty { null },
                            groupId = s.groupId,
                            mediaPaths = finalFilenames,
                        )
                    )
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun finalizeMediaFiles(paths: List<String>, poiId: String): List<String> {
        val poi = poiRepository.observeAll().let {
            // Get current name from in-memory state (needed for folder path)
            _state.value.name.trim().ifEmpty { "poi" }
        }
        val mediaDir = storageManager.getPoiMediaDir(poi, poiId)
        return paths.map { path ->
            val file = File(path)
            if (file.exists() && file.absolutePath.startsWith(storageManager.getTempDir().absolutePath)) {
                val dest = File(mediaDir, file.name)
                file.renameTo(dest)
                dest.name
            } else {
                file.name
            }
        }
    }
}
