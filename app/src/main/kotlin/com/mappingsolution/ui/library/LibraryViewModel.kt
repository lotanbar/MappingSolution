package com.mappingsolution.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mappingsolution.data.fs.BulkPoiRepository
import com.mappingsolution.data.fs.ExportRepository
import com.mappingsolution.data.fs.GroupFileRepository
import com.mappingsolution.data.fs.ImportRepository
import com.mappingsolution.data.fs.ImportResult
import com.mappingsolution.data.fs.PoiFileRepository
import com.mappingsolution.data.fs.RouteFileRepository
import com.mappingsolution.data.model.Group
import com.mappingsolution.data.model.Poi
import com.mappingsolution.data.model.Route
import com.mappingsolution.data.places.GOOGLE_PLACES_GROUP_ID
import com.mappingsolution.data.places.GooglePlacesRepository
import com.mappingsolution.data.places.OSM_POI_GROUP_ID
import com.mappingsolution.data.places.OsmPoiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeleteGroupResult {
    object Done : DeleteGroupResult()
    data class HasItems(val poiCount: Int) : DeleteGroupResult()
}

sealed interface LibrarySelectionMode {
    data object None : LibrarySelectionMode
    data class GroupSelection(val selectedIds: Set<String> = emptySet()) : LibrarySelectionMode
    data class RowSelection(val selectedIds: Set<String> = emptySet()) : LibrarySelectionMode
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val groupRepository: GroupFileRepository,
    private val poiRepository: PoiFileRepository,
    private val routeRepository: RouteFileRepository,
    private val importRepository: ImportRepository,
    private val exportRepository: ExportRepository,
    private val googlePlacesRepository: GooglePlacesRepository,
    private val osmPoiRepository: OsmPoiRepository,
    private val bulkPoiRepository: BulkPoiRepository,
) : ViewModel() {

    // ── Raw data ──────────────────────────────────────────────────────────

    private val _allGroups = groupRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _allPois = poiRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _allRoutes = routeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All groups unfiltered — used for the group-picker dialog in un-orphan. */
    val allGroupsUnfiltered: StateFlow<List<Group>> = _allGroups

    // ── Places group counts + visibility ─────────────────────────────────

    val googlePlaceCount: StateFlow<Int> = googlePlacesRepository.pois
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val osmPoiCount: StateFlow<Int> = osmPoiRepository.pois
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val googlePlacesGroup: StateFlow<Group?> = _allGroups
        .map { groups -> groups.find { it.id == GOOGLE_PLACES_GROUP_ID } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val osmPoiGroup: StateFlow<Group?> = _allGroups
        .map { groups -> groups.find { it.id == OSM_POI_GROUP_ID } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleGooglePlacesVisibility() {
        val group = googlePlacesGroup.value ?: return
        viewModelScope.launch { groupRepository.setVisibility(group.id, !group.isVisible) }
    }

    fun toggleOsmPoisVisibility() {
        val group = osmPoiGroup.value ?: return
        viewModelScope.launch { groupRepository.setVisibility(group.id, !group.isVisible) }
    }

    // ── Search ────────────────────────────────────────────────────────────

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ── Collapse state ────────────────────────────────────────────────────

    private val _expandedGroups = MutableStateFlow<Set<String>>(emptySet())
    val expandedGroups: StateFlow<Set<String>> = _expandedGroups.asStateFlow()

    // ── Selection mode ────────────────────────────────────────────────────

    private val _selectionMode = MutableStateFlow<LibrarySelectionMode>(LibrarySelectionMode.None)
    val selectionMode: StateFlow<LibrarySelectionMode> = _selectionMode.asStateFlow()

    // ── Filtered lists ────────────────────────────────────────────────────

    /** Groups shown in the list: excludes the seeded places groups (rendered separately). */
    val filteredGroups: StateFlow<List<Group>> = combine(
        _allGroups, _allPois, _searchQuery,
    ) { groups, pois, query ->
        val userGroups = groups.filter { it.id != GOOGLE_PLACES_GROUP_ID && it.id != OSM_POI_GROUP_ID }
        if (query.isBlank()) return@combine userGroups
        val poisByGroup = pois.groupBy { it.groupId }
        userGroups.filter { g ->
            g.name.contains(query, ignoreCase = true) ||
                poisByGroup[g.id]?.any { it.name.contains(query, ignoreCase = true) } == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All grouped POIs keyed by groupId (unfiltered by search, for collapse rendering). */
    val poisByGroup: StateFlow<Map<String, List<Poi>>> = _allPois
        .map { pois -> pois.filter { it.groupId != null }.groupBy { it.groupId!! } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Orphaned POIs (no group) filtered by search. */
    val filteredOrphanedPois: StateFlow<List<Poi>> = combine(
        _allPois, _searchQuery,
    ) { pois, query ->
        val orphans = pois.filter { it.groupId == null }
        if (query.isBlank()) orphans else orphans.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Routes filtered by search. */
    val filteredRoutes: StateFlow<List<Route>> = combine(
        _allRoutes, _searchQuery,
    ) { routes, query ->
        if (query.isBlank()) routes else routes.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Row-selection action availability ─────────────────────────────────

    /** True when selection contains a POI that already has a group (can be orphaned). */
    val canOrphanSelection: StateFlow<Boolean> = combine(
        _selectionMode, _allPois,
    ) { mode, pois ->
        val ids = (mode as? LibrarySelectionMode.RowSelection)?.selectedIds ?: return@combine false
        pois.any { it.id in ids && it.groupId != null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** True when selection contains an orphaned POI (can be moved to a group). */
    val canUnorphanSelection: StateFlow<Boolean> = combine(
        _selectionMode, _allPois,
    ) { mode, pois ->
        val ids = (mode as? LibrarySelectionMode.RowSelection)?.selectedIds ?: return@combine false
        pois.any { it.id in ids && it.groupId == null }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ── Search ────────────────────────────────────────────────────────────

    fun onSearchQuery(query: String) { _searchQuery.value = query }

    // ── Collapse ──────────────────────────────────────────────────────────

    fun toggleCollapse(groupId: String) {
        _expandedGroups.update { if (groupId in it) it - groupId else it + groupId }
    }

    // ── Selection ─────────────────────────────────────────────────────────

    fun enterGroupSelection(groupId: String) {
        _selectionMode.value = LibrarySelectionMode.GroupSelection(setOf(groupId))
    }

    fun enterRowSelection(id: String) {
        _selectionMode.value = LibrarySelectionMode.RowSelection(setOf(id))
    }

    fun toggleGroupSelection(groupId: String) {
        val current = _selectionMode.value as? LibrarySelectionMode.GroupSelection ?: return
        val updated = if (groupId in current.selectedIds) current.selectedIds - groupId
                      else current.selectedIds + groupId
        _selectionMode.value = if (updated.isEmpty()) LibrarySelectionMode.None
                                else current.copy(selectedIds = updated)
    }

    fun toggleRowSelection(id: String) {
        val current = _selectionMode.value as? LibrarySelectionMode.RowSelection ?: return
        val updated = if (id in current.selectedIds) current.selectedIds - id
                      else current.selectedIds + id
        _selectionMode.value = if (updated.isEmpty()) LibrarySelectionMode.None
                                else current.copy(selectedIds = updated)
    }

    fun clearSelection() { _selectionMode.value = LibrarySelectionMode.None }

    // ── Visibility ────────────────────────────────────────────────────────

    fun toggleGroupVisibility(group: Group) {
        viewModelScope.launch { groupRepository.setVisibility(group.id, !group.isVisible) }
    }

    fun togglePoiVisibility(poi: Poi) {
        viewModelScope.launch { poiRepository.update(poi.copy(isVisible = !poi.isVisible)) }
    }

    fun toggleRouteVisibility(route: Route) {
        viewModelScope.launch { routeRepository.update(route.copy(isVisible = !route.isVisible)) }
    }

    // ── Group multi-select actions ────────────────────────────────────────

    /** Delete the selected groups and all their POIs. */
    fun deleteSelectedGroupsWithItems() {
        val ids = (_selectionMode.value as? LibrarySelectionMode.GroupSelection)?.selectedIds ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val selectedGroups = _allGroups.value.filter { it.id in ids }
            val bulkIds = selectedGroups.filter { it.isBulk }.map { it.id }.toSet()
            val regularIds = ids - bulkIds

            // Delete regular POIs the normal way
            val poisToDelete = _allPois.value.filter { it.groupId in regularIds }.map { it.id }
            if (poisToDelete.isNotEmpty()) {
                poiRepository.deleteByIds(poisToDelete) { done, total ->
                    reportProgress("Deleting…", done, total)
                }
            }
            // Delete bulk group folders wholesale
            selectedGroups.filter { it.isBulk }.forEach { group ->
                bulkPoiRepository.deleteGroup(group.name, group.id)
            }
            selectedGroups.forEach { groupRepository.delete(it) }
            clearProgress()
            clearSelection()
        }
    }

    /** Delete the selected groups; their POIs become orphaned. */
    fun orphanSelectedGroups() {
        val ids = (_selectionMode.value as? LibrarySelectionMode.GroupSelection)?.selectedIds ?: return
        viewModelScope.launch {
            _isBusy.value = true
            reportProgress("Orphaning…", 0, 0)
            val selectedGroups = _allGroups.value.filter { it.id in ids }
            val regularIds = selectedGroups.filter { !it.isBulk }.map { it.id }.toSet()
            // Bulk groups have no individual POIs in poiRepository — just delete the group record
            poiRepository.orphan(_allPois.value.filter { it.groupId in regularIds }.map { it.id })
            selectedGroups.forEach { groupRepository.delete(it) }
            clearProgress()
            clearSelection()
        }
    }

    // ── Row multi-select actions ───────────────────────────────────────────

    /** Delete selected POIs and/or routes. */
    fun deleteSelectedRows() {
        val ids = (_selectionMode.value as? LibrarySelectionMode.RowSelection)?.selectedIds ?: return
        viewModelScope.launch {
            poiRepository.deleteByIds(ids.toList())
            routeRepository.deleteByIds(ids.toList())
            clearSelection()
        }
    }

    /** Remove group assignment from all grouped POIs in the selection. */
    fun orphanSelectedRows() {
        val ids = (_selectionMode.value as? LibrarySelectionMode.RowSelection)?.selectedIds ?: return
        viewModelScope.launch {
            val grouped = _allPois.value.filter { it.id in ids && it.groupId != null }.map { it.id }
            if (grouped.isNotEmpty()) poiRepository.orphan(grouped)
            clearSelection()
        }
    }

    /** Move orphaned POIs in the selection to the given group. */
    fun moveSelectedRowsToGroup(groupId: String) {
        val ids = (_selectionMode.value as? LibrarySelectionMode.RowSelection)?.selectedIds ?: return
        viewModelScope.launch {
            val orphans = _allPois.value.filter { it.id in ids && it.groupId == null }.map { it.id }
            if (orphans.isNotEmpty()) poiRepository.moveToGroup(orphans, groupId)
            clearSelection()
        }
    }

    // ── Import ────────────────────────────────────────────────────────────

    private val _isBusy = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _importingFolderName = MutableStateFlow<String?>(null)
    val importingFolderName: StateFlow<String?> = _importingFolderName.asStateFlow()

    private val _importProgressText = MutableStateFlow("")
    val importProgressText: StateFlow<String> = _importProgressText.asStateFlow()

    private val _importProgressFraction = MutableStateFlow(0f)
    val importProgressFraction: StateFlow<Float> = _importProgressFraction.asStateFlow()

    private fun reportProgress(phase: String, done: Int, total: Int) {
        _importProgressText.value = if (total > 0) "$phase $done / $total" else phase
        _importProgressFraction.value = if (total > 0) done.toFloat() / total else 0f
    }

    private fun clearProgress() {
        _importProgressText.value = ""
        _importProgressFraction.value = 0f
        _importingFolderName.value = null
        _isBusy.value = false
    }

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    fun importFromFolder(path: String) {
        viewModelScope.launch {
            _isBusy.value = true
            _importingFolderName.value = java.io.File(path).name
            reportProgress("Starting…", 0, 0)
            _importResult.value = importRepository.importFolder(path) { phase, done, total ->
                reportProgress(phase, done, total)
            }
            clearProgress()
        }
    }

    fun dismissImportResult() { _importResult.value = null }

    // ── Export ────────────────────────────────────────────────────────────

    private val _exportUri = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val exportUri = _exportUri.asSharedFlow()

    fun exportSelectedGroups() {
        val ids = (_selectionMode.value as? LibrarySelectionMode.GroupSelection)?.selectedIds ?: return
        viewModelScope.launch {
            val uri = exportRepository.exportGroups(ids) ?: return@launch
            _exportUri.tryEmit(uri)
        }
    }

    fun exportSelectedRows() {
        val ids = (_selectionMode.value as? LibrarySelectionMode.RowSelection)?.selectedIds ?: return
        viewModelScope.launch {
            val uri = exportRepository.exportRows(ids) ?: return@launch
            _exportUri.tryEmit(uri)
        }
    }
}
