package com.mappingsolution.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.data.model.Group
import com.mappingsolution.data.model.Poi
import com.mappingsolution.data.model.Route
import com.mappingsolution.ui.common.IconCatalog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onCreateGroup: () -> Unit,
    onEditGroup: (String) -> Unit,
    onEditPoi: (String) -> Unit,
    onEditRoute: (String) -> Unit,
    onContinueRecording: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredGroups by viewModel.filteredGroups.collectAsState()
    val poisByGroup by viewModel.poisByGroup.collectAsState()
    val filteredOrphanedPois by viewModel.filteredOrphanedPois.collectAsState()
    val filteredRoutes by viewModel.filteredRoutes.collectAsState()
    val expandedGroups by viewModel.expandedGroups.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val canOrphan by viewModel.canOrphanSelection.collectAsState()
    val canUnorphan by viewModel.canUnorphanSelection.collectAsState()
    val allGroups by viewModel.allGroupsUnfiltered.collectAsState()

    // ── Local dialog state ────────────────────────────────────────────────
    var showDeleteGroupsDialog by remember { mutableStateOf(false) }
    var showDeleteRowsDialog by remember { mutableStateOf(false) }
    var showGroupPickerDialog by remember { mutableStateOf(false) }
    var incompleteRoute by remember { mutableStateOf<Route?>(null) }

    // Back press in selection mode clears it instead of navigating away
    BackHandler(enabled = selectionMode !is LibrarySelectionMode.None) {
        viewModel.clearSelection()
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    if (showDeleteGroupsDialog) {
        val count = (selectionMode as? LibrarySelectionMode.GroupSelection)?.selectedIds?.size ?: 0
        DestructiveCooldownDialog(
            title = "Delete $count group${if (count != 1) "s" else ""}",
            text = "This will permanently delete the selected group${if (count != 1) "s" else ""} and all their POIs. This cannot be undone.",
            onConfirm = { showDeleteGroupsDialog = false; viewModel.deleteSelectedGroupsWithItems() },
            onDismiss = { showDeleteGroupsDialog = false },
        )
    }

    if (showDeleteRowsDialog) {
        val count = (selectionMode as? LibrarySelectionMode.RowSelection)?.selectedIds?.size ?: 0
        DestructiveCooldownDialog(
            title = "Delete $count item${if (count != 1) "s" else ""}",
            text = "This will permanently delete the selected item${if (count != 1) "s" else ""}. This cannot be undone.",
            onConfirm = { showDeleteRowsDialog = false; viewModel.deleteSelectedRows() },
            onDismiss = { showDeleteRowsDialog = false },
        )
    }

    if (showGroupPickerDialog) {
        GroupPickerDialog(
            groups = allGroups,
            onGroupPicked = { groupId ->
                viewModel.moveSelectedRowsToGroup(groupId)
                showGroupPickerDialog = false
            },
            onDismiss = { showGroupPickerDialog = false },
        )
    }

    incompleteRoute?.let { route ->
        IncompleteRouteDialog(
            route = route,
            onContinue = {
                incompleteRoute = null
                onContinueRecording(route.id)
            },
            onDismiss = { incompleteRoute = null },
        )
    }

    Scaffold(
        topBar = {
            when (val mode = selectionMode) {
                is LibrarySelectionMode.None -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 3.dp,
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            SearchBar(
                                inputField = {
                                    SearchBarDefaults.InputField(
                                        query = searchQuery,
                                        onQueryChange = viewModel::onSearchQuery,
                                        onSearch = {},
                                        expanded = false,
                                        onExpandedChange = {},
                                        placeholder = { Text("Search groups, POIs, routes…") },
                                    )
                                },
                                expanded = false,
                                onExpandedChange = {},
                                modifier = Modifier.weight(1f),
                                content = {},
                            )
                            IconButton(onClick = onCreateGroup) {
                                Icon(Icons.Default.Add, contentDescription = "Create group")
                            }
                        }
                    }
                }
                is LibrarySelectionMode.GroupSelection -> {
                    TopAppBar(
                        title = { Text("${mode.selectedIds.size} group${if (mode.selectedIds.size != 1) "s" else ""} selected") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                            }
                        },
                        actions = {
                            // Orphan action: delete groups, keep POIs
                            IconButton(onClick = { viewModel.orphanSelectedGroups() }) {
                                Icon(Icons.Default.FolderOff, contentDescription = "Orphan items")
                            }
                            // Destructive delete: groups + all POIs
                            IconButton(onClick = { showDeleteGroupsDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete groups and items",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
                is LibrarySelectionMode.RowSelection -> {
                    TopAppBar(
                        title = { Text("${mode.selectedIds.size} item${if (mode.selectedIds.size != 1) "s" else ""} selected") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                            }
                        },
                        actions = {
                            // Orphan: remove group from selected grouped POIs
                            if (canOrphan) {
                                IconButton(onClick = { viewModel.orphanSelectedRows() }) {
                                    Icon(Icons.Default.FolderOff, contentDescription = "Ungroup selected POIs")
                                }
                            }
                            // Un-orphan: move orphaned POIs to a group
                            if (canUnorphan) {
                                IconButton(onClick = { showGroupPickerDialog = true }) {
                                    Icon(Icons.Default.DriveFileMove, contentDescription = "Move to group")
                                }
                            }
                            // Delete selected rows
                            IconButton(onClick = { showDeleteRowsDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete selected",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            // ── Groups ────────────────────────────────────────────────────
            if (filteredGroups.isNotEmpty()) {
                item { SectionHeader("Groups") }

                filteredGroups.forEach { group ->
                    val isExpanded = group.id in expandedGroups
                    val isGroupSelected = (selectionMode as? LibrarySelectionMode.GroupSelection)
                        ?.selectedIds?.contains(group.id) == true

                    item(key = "group-${group.id}") {
                        GroupHeaderRow(
                            group = group,
                            isExpanded = isExpanded,
                            isSelected = isGroupSelected,
                            selectionMode = selectionMode,
                            onTap = {
                                when (selectionMode) {
                                    is LibrarySelectionMode.None -> viewModel.toggleCollapse(group.id)
                                    is LibrarySelectionMode.GroupSelection -> viewModel.toggleGroupSelection(group.id)
                                    is LibrarySelectionMode.RowSelection -> Unit
                                }
                            },
                            onLongPress = {
                                if (selectionMode is LibrarySelectionMode.None) {
                                    viewModel.enterGroupSelection(group.id)
                                }
                            },
                            onEditTap = { onEditGroup(group.id) },
                            onToggleVisibility = { viewModel.toggleGroupVisibility(group) },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    if (isExpanded) {
                        val groupPois = poisByGroup[group.id].orEmpty()
                        items(groupPois, key = { "gpoi-${it.id}" }) { poi ->
                            val isPoiSelected = (selectionMode as? LibrarySelectionMode.RowSelection)
                                ?.selectedIds?.contains(poi.id) == true
                            PoiRow(
                                poi = poi,
                                isSelected = isPoiSelected,
                                selectionMode = selectionMode,
                                indented = true,
                                onTap = {
                                    when (selectionMode) {
                                        is LibrarySelectionMode.None -> onEditPoi(poi.id)
                                        is LibrarySelectionMode.RowSelection -> viewModel.toggleRowSelection(poi.id)
                                        is LibrarySelectionMode.GroupSelection -> Unit
                                    }
                                },
                                onLongPress = {
                                    if (selectionMode is LibrarySelectionMode.None) {
                                        viewModel.enterRowSelection(poi.id)
                                    }
                                },
                                onToggleVisibility = { viewModel.togglePoiVisibility(poi) },
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp))
                        }
                    }
                }
            }

            // ── Orphaned POIs ─────────────────────────────────────────────
            if (filteredOrphanedPois.isNotEmpty()) {
                item { SectionHeader("Unassigned POIs") }
                items(filteredOrphanedPois, key = { "orphan-${it.id}" }) { poi ->
                    val isSelected = (selectionMode as? LibrarySelectionMode.RowSelection)
                        ?.selectedIds?.contains(poi.id) == true
                    PoiRow(
                        poi = poi,
                        isSelected = isSelected,
                        selectionMode = selectionMode,
                        indented = false,
                        onTap = {
                            when (selectionMode) {
                                is LibrarySelectionMode.None -> onEditPoi(poi.id)
                                is LibrarySelectionMode.RowSelection -> viewModel.toggleRowSelection(poi.id)
                                is LibrarySelectionMode.GroupSelection -> Unit
                            }
                        },
                        onLongPress = {
                            if (selectionMode is LibrarySelectionMode.None) {
                                viewModel.enterRowSelection(poi.id)
                            }
                        },
                        onToggleVisibility = { viewModel.togglePoiVisibility(poi) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // ── Routes ────────────────────────────────────────────────────
            if (filteredRoutes.isNotEmpty()) {
                item { SectionHeader("Routes") }
                items(filteredRoutes, key = { "route-${it.id}" }) { route ->
                    val isSelected = (selectionMode as? LibrarySelectionMode.RowSelection)
                        ?.selectedIds?.contains(route.id) == true
                    RouteRow(
                        route = route,
                        isSelected = isSelected,
                        selectionMode = selectionMode,
                        onTap = {
                            when (selectionMode) {
                                is LibrarySelectionMode.None -> onEditRoute(route.id)
                                is LibrarySelectionMode.RowSelection -> viewModel.toggleRowSelection(route.id)
                                is LibrarySelectionMode.GroupSelection -> Unit
                            }
                        },
                        onLongPress = {
                            if (selectionMode is LibrarySelectionMode.None) {
                                viewModel.enterRowSelection(route.id)
                            }
                        },
                        onToggleVisibility = { viewModel.toggleRouteVisibility(route) },
                        onIncompleteIconTap = { incompleteRoute = route },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // ── Empty state ───────────────────────────────────────────────
            if (filteredGroups.isEmpty() && filteredOrphanedPois.isEmpty() && filteredRoutes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (searchQuery.isBlank()) Icons.Default.FolderOpen else Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isBlank()) "Nothing here yet" else "No results for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            if (searchQuery.isBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Record a route or tap + to create a group",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

// ── Group header row ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupHeaderRow(
    group: Group,
    isExpanded: Boolean,
    isSelected: Boolean,
    selectionMode: LibrarySelectionMode,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onEditTap: () -> Unit,
    onToggleVisibility: () -> Unit,
) {
    val groupColor = parseHexColor(group.color)
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    ListItem(
        colors = ListItemDefaults.colors(containerColor = containerColor),
        modifier = Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress),
        headlineContent = { Text(group.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = group.description?.let {
            { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
        },
        leadingContent = {
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .drawBehind { drawCircle(primaryColor) },
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = onPrimaryColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .drawBehind { drawCircle(groupColor) },
                ) {
                    Icon(
                        imageVector = IconCatalog.iconVector(group.iconKey),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode is LibrarySelectionMode.None) {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (group.isVisible) Icons.Default.Visibility
                                          else Icons.Default.VisibilityOff,
                            contentDescription = if (group.isVisible) "Hide" else "Show",
                            tint = if (group.isVisible) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

// ── POI row ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PoiRow(
    poi: Poi,
    isSelected: Boolean,
    selectionMode: LibrarySelectionMode,
    indented: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToggleVisibility: () -> Unit,
) {
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    ListItem(
        colors = ListItemDefaults.colors(containerColor = containerColor),
        modifier = Modifier
            .then(if (indented) Modifier.padding(start = 24.dp) else Modifier)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        headlineContent = { Text(poi.name) },
        supportingContent = poi.description?.let {
            { Text(it, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        leadingContent = {
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
        },
        trailingContent = if (selectionMode is LibrarySelectionMode.None) {
            {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (poi.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (poi.isVisible) "Hide" else "Show",
                        tint = if (poi.isVisible) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
            }
        } else null,
    )
}

// ── Route row ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RouteRow(
    route: Route,
    isSelected: Boolean,
    selectionMode: LibrarySelectionMode,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onToggleVisibility: () -> Unit,
    onIncompleteIconTap: () -> Unit,
) {
    val routeColor = parseHexColor(route.color)
    val isIncomplete = !route.didUserTapStop
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surface

    ListItem(
        colors = ListItemDefaults.colors(containerColor = containerColor),
        modifier = Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress),
        headlineContent = { Text(route.name) },
        supportingContent = route.description?.let {
            { Text(it, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        leadingContent = {
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(routeColor),
                )
            }
        },
        trailingContent = if (selectionMode is LibrarySelectionMode.None) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isIncomplete) {
                        IconButton(onClick = onIncompleteIconTap) {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = "Incomplete recording",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (route.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (route.isVisible) "Hide" else "Show",
                            tint = if (route.isVisible) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        } else null,
    )
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun DestructiveCooldownDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var seconds by remember { mutableIntStateOf(10) }
    LaunchedEffect(Unit) {
        while (seconds > 0) { delay(1_000L); seconds-- }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = seconds == 0) {
                Text(
                    if (seconds > 0) "Delete ($seconds)" else "Delete",
                    color = if (seconds == 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GroupPickerDialog(
    groups: List<Group>,
    onGroupPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to group") },
        text = {
            Column {
                if (groups.isEmpty()) {
                    Text("No groups available. Create a group first.")
                } else {
                    groups.forEach { group ->
                        ListItem(
                            headlineContent = { Text(group.name) },
                            leadingContent = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .drawBehind { drawCircle(parseHexColor(group.color)) },
                                ) {
                                    Icon(
                                        imageVector = IconCatalog.iconVector(group.iconKey),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onGroupPicked(group.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun IncompleteRouteDialog(
    route: Route,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Incomplete recording") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("\"${route.name}\" was not stopped properly.")
                Spacer(Modifier.height(4.dp))
                Text("Distance: ${formatDistance(route.distanceMeters)}")
                Text("Duration: ${formatDuration(route.durationSec)}")
                Text("Started: ${formatDate(route.startedAt)}")
                Spacer(Modifier.height(8.dp))
                Text("Would you like to continue this recording?")
            }
        },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun parseHexColor(hex: String): Color = try {
    val cleaned = hex.trimStart('#')
    val long = cleaned.toLong(16)
    val a = if (cleaned.length == 8) ((long shr 24) and 0xFF) / 255f else 1f
    val r = ((long shr 16) and 0xFF) / 255f
    val g = ((long shr 8) and 0xFF) / 255f
    val b = (long and 0xFF) / 255f
    Color(r, g, b, a)
} catch (_: Exception) { Color.Gray }

private fun formatDistance(meters: Double): String {
    return if (meters < 1000) "%.0f m".format(meters)
    else "%.2f km".format(meters / 1000)
}

private fun formatDuration(seconds: Long): String {
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epochMs))
