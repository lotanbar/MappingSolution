package com.mappingsolution.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.fs.GroupFileRepository
import com.mappingsolution.data.fs.PoiFileRepository
import com.mappingsolution.data.fs.RouteFileRepository
import com.mappingsolution.data.model.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeleteGroupResult {
    object Done : DeleteGroupResult()
    data class HasItems(val poiCount: Int) : DeleteGroupResult()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val groupRepository: GroupFileRepository,
    private val poiRepository: PoiFileRepository,
    private val routeRepository: RouteFileRepository,
) : ViewModel() {

    val groups: StateFlow<List<Group>> = groupRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val orphanedPois = poiRepository.observeOrphans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allRoutes = routeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleVisibility(group: Group) {
        viewModelScope.launch {
            groupRepository.update(group.copy(isVisible = !group.isVisible))
        }
    }

    fun requestDelete(group: Group, onResult: (DeleteGroupResult) -> Unit) {
        viewModelScope.launch {
            val poiCount = poiRepository.countByGroup(group.id)
            if (poiCount > 0) {
                onResult(DeleteGroupResult.HasItems(poiCount))
            } else {
                groupRepository.delete(group)
                onResult(DeleteGroupResult.Done)
            }
        }
    }

    fun deleteGroupOrphanItems(group: Group) {
        viewModelScope.launch {
            poiRepository.orphan(
                poiRepository.observeByGroup(group.id)
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                    .value.map { it.id }
            )
            groupRepository.delete(group)
        }
    }
}
