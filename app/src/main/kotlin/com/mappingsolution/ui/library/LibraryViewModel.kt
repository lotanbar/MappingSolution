package com.mappingsolution.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.db.entity.GroupEntity
import com.mappingsolution.data.repository.GroupRepository
import com.mappingsolution.data.repository.PoiRepository
import com.mappingsolution.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeleteGroupResult {
    object Done : DeleteGroupResult()
    data class HasItems(val poiCount: Int, val routeCount: Int) : DeleteGroupResult()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val poiRepository: PoiRepository,
    private val routeRepository: RouteRepository,
) : ViewModel() {

    val groups: StateFlow<List<GroupEntity>> = groupRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleVisibility(group: GroupEntity) {
        viewModelScope.launch {
            groupRepository.update(group.copy(isVisible = !group.isVisible))
        }
    }

    /** Checks for items before deleting. Calls [onResult] with [DeleteGroupResult.HasItems] if
     *  the group is not empty, or [DeleteGroupResult.Done] if already deleted. */
    fun requestDelete(group: GroupEntity, onResult: (DeleteGroupResult) -> Unit) {
        viewModelScope.launch {
            val poiCount = poiRepository.countByGroup(group.id)
            val routeCount = routeRepository.countByGroup(group.id)
            if (poiCount + routeCount > 0) {
                onResult(DeleteGroupResult.HasItems(poiCount, routeCount))
            } else {
                groupRepository.delete(group)
                onResult(DeleteGroupResult.Done)
            }
        }
    }

    /** Deletes the group; Room's FK SET_NULL propagates to its POIs and routes (orphaning them). */
    fun deleteGroupOrphanItems(group: GroupEntity) {
        viewModelScope.launch { groupRepository.delete(group) }
    }
}
