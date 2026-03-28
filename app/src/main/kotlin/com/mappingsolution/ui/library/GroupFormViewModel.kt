package com.mappingsolution.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.db.entity.GroupEntity
import com.mappingsolution.data.repository.DuplicateFieldError
import com.mappingsolution.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupFormState(
    val name: String = "",
    val description: String = "",
    val iconKey: String = "place",
    val color: String = "#FF2196F3",
    val nameError: String? = null,
    val descriptionError: String? = null,
    val iconError: String? = null,
    val colorError: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val savedSuccessfully: Boolean = false,
)

@HiltViewModel
class GroupFormViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: Long? = savedStateHandle.get<Long>("groupId")?.takeIf { it > 0 }

    val isEditing: Boolean get() = groupId != null

    private val _state = MutableStateFlow(GroupFormState(isLoading = groupId != null))
    val state: StateFlow<GroupFormState> = _state.asStateFlow()

    init {
        groupId?.let { id ->
            viewModelScope.launch {
                val group = groupRepository.getById(id)
                if (group != null) {
                    _state.update {
                        it.copy(
                            name = group.name,
                            description = group.description ?: "",
                            iconKey = group.iconKey,
                            color = group.color,
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
    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value, descriptionError = null) }
    fun onIconChange(key: String) = _state.update { it.copy(iconKey = key, iconError = null) }
    fun onColorChange(hex: String) = _state.update { it.copy(color = hex, colorError = null) }

    fun save(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = "Name is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val entity = GroupEntity(
                id = groupId ?: 0,
                name = s.name.trim(),
                description = s.description.trim().ifEmpty { null },
                iconKey = s.iconKey,
                color = s.color,
            )
            val result = if (groupId == null) groupRepository.insert(entity)
                         else groupRepository.update(entity)

            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    _state.update { it.copy(isSaving = false) }
                    when (error) {
                        is DuplicateFieldError.Name        -> _state.update { it.copy(nameError = error.message) }
                        is DuplicateFieldError.Description -> _state.update { it.copy(descriptionError = error.message) }
                        is DuplicateFieldError.Icon        -> _state.update { it.copy(iconError = error.message) }
                        is DuplicateFieldError.Color       -> _state.update { it.copy(colorError = error.message) }
                        else                               -> _state.update { it.copy(nameError = error.message) }
                    }
                }
            )
        }
    }
}
