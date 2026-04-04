package com.mappingsolution.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.data.db.entity.GroupEntity
import com.mappingsolution.ui.common.IconCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateBack: () -> Unit,
    onCreateGroup: () -> Unit,
    onEditGroup: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsState()
    val orphanedPois by viewModel.orphanedPois.collectAsState()
    val allRoutes by viewModel.allRoutes.collectAsState()
    var deleteTarget by remember { mutableStateOf<GroupEntity?>(null) }
    var deleteWithItems by remember { mutableStateOf<Pair<GroupEntity, DeleteGroupResult.HasItems>?>(null) }

    // Simple delete confirmation (no items)
    deleteTarget?.let { group ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete group") },
            text = { Text("Delete \"${group.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.requestDelete(group) { result ->
                        when (result) {
                            is DeleteGroupResult.Done -> { /* nothing, list updates reactively */ }
                            is DeleteGroupResult.HasItems -> {
                                deleteWithItems = group to result
                            }
                        }
                    }
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }

    // Delete confirmation when group has items
    deleteWithItems?.let { (group, info) ->
        AlertDialog(
            onDismissRequest = { deleteWithItems = null },
            title = { Text("Delete group") },
            text = {
                Text("\"${group.name}\" has ${info.poiCount} POI(s). Delete the group and orphan its items?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroupOrphanItems(group)
                    deleteWithItems = null
                }) { Text("Delete & Orphan", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteWithItems = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateGroup) {
                        Icon(Icons.Default.Add, contentDescription = "Create group")
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            if (groups.isNotEmpty()) {
                items(groups, key = { it.id }) { group ->
                    GroupRow(
                        group = group,
                        onToggleVisibility = { viewModel.toggleVisibility(group) },
                        onEdit = { onEditGroup(group.id) },
                        onDelete = { deleteTarget = group },
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            if (orphanedPois.isNotEmpty()) {
                item {
                    Text(
                        "Unassigned POIs",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(orphanedPois, key = { "poi-${it.id}" }) { poi ->
                    ListItem(
                        headlineContent = { Text(poi.name) },
                        leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    )
                }
            }
            if (allRoutes.isNotEmpty()) {
                item {
                    Text(
                        "Routes",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(allRoutes, key = { "route-${it.id}" }) { route ->
                    val routeColor = try {
                        Color(android.graphics.Color.parseColor(route.color))
                    } catch (_: Exception) {
                        Color(0xFFFF5722.toInt())
                    }
                    ListItem(
                        headlineContent = { Text(route.name) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(routeColor),
                            )
                        },
                    )
                }
            }
            if (groups.isEmpty() && orphanedPois.isEmpty() && allRoutes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Nothing here yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
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

@Composable
private fun GroupRow(
    group: GroupEntity,
    onToggleVisibility: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val groupColor = parseGroupColor(group.color)

    ListItem(
        headlineContent = { Text(group.name, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = group.description?.let {
            { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
        },
        leadingContent = {
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
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (group.isVisible) Icons.Default.Visibility
                                      else Icons.Default.VisibilityOff,
                        contentDescription = if (group.isVisible) "Hide group" else "Show group",
                        tint = if (group.isVisible) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit group")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete group",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
    )
}

private fun parseGroupColor(hex: String): Color {
    return try {
        val cleaned = hex.trimStart('#')
        val long = cleaned.toLong(16)
        val a = if (cleaned.length == 8) ((long shr 24) and 0xFF) / 255f else 1f
        val r = ((long shr 16) and 0xFF) / 255f
        val g = ((long shr 8) and 0xFF) / 255f
        val b = (long and 0xFF) / 255f
        Color(r, g, b, a)
    } catch (_: Exception) {
        Color.Gray
    }
}
