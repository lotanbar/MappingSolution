package com.mappingsolution.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.fs.RouteFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteFinalizeState(
    val routeId: String = "",
    val name: String = "",
    val description: String = "",
    val color: String = "#FFFF5722",
    val distanceMeters: Double = 0.0,
    val isSaving: Boolean = false,
)

@HiltViewModel
class RouteFinalizeViewModel @Inject constructor(
    private val routeRepository: RouteFileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RouteFinalizeState())
    val state: StateFlow<RouteFinalizeState> = _state.asStateFlow()

    fun load(routeId: String) {
        viewModelScope.launch {
            val route = routeRepository.getById(routeId) ?: return@launch
            _state.value = RouteFinalizeState(
                routeId = route.id,
                name = route.name,
                description = route.description ?: "",
                color = route.color,
                distanceMeters = route.distanceMeters,
            )
        }
    }

    fun onNameChange(value: String) { _state.value = _state.value.copy(name = value) }
    fun onDescriptionChange(value: String) { _state.value = _state.value.copy(description = value) }
    fun onColorChange(color: String) { _state.value = _state.value.copy(color = color) }

    fun save(onDone: () -> Unit) {
        val st = _state.value
        if (st.routeId.isEmpty()) return
        viewModelScope.launch {
            _state.value = st.copy(isSaving = true)
            routeRepository.updateFields(
                id = st.routeId,
                name = st.name.trim(),
                description = st.description.trim().ifEmpty { null },
                color = st.color,
            )
            onDone()
        }
    }
}
