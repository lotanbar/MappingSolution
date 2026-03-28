package com.mappingsolution.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.db.entity.GroupEntity
import com.mappingsolution.data.db.entity.PoiEntity
import com.mappingsolution.data.db.entity.RouteEntity
import com.mappingsolution.data.repository.GroupRepository
import com.mappingsolution.data.repository.PoiRepository
import com.mappingsolution.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    groupRepository: GroupRepository,
    poiRepository: PoiRepository,
    routeRepository: RouteRepository
) : ViewModel() {

    val groups: StateFlow<List<GroupEntity>> = groupRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pois: StateFlow<List<PoiEntity>> = poiRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routes: StateFlow<List<RouteEntity>> = routeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
