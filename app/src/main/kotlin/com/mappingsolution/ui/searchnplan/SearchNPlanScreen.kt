package com.mappingsolution.ui.searchnplan

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.ui.searchnplan.components.SearchResultRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchNPlanScreen(
    onNavigateBack: () -> Unit,
    viewModel: SearchNPlanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.results.collectAsState()
    val destinations by viewModel.destinations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeRowIndex by viewModel.activeRowIndex.collectAsState()

    var showSavePlanDialog by remember { mutableStateOf(false) }
    var planNameInput by remember { mutableStateOf("") }

    // Navigate back after successful save
    LaunchedEffect(Unit) {
        viewModel.savedEvent.collect { onNavigateBack() }
    }

    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    val destinationsList = remember { derivedStateOf { destinations } }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val list = destinationsList.value
        val fromIndex = list.indexOfFirst { "dest_${it.id}" == from.key }
        val toIndex = list.indexOfFirst { "dest_${it.id}" == to.key }
        if (fromIndex != -1 && toIndex != -1) viewModel.moveDestination(fromIndex, toIndex)
    }

    // Drag handles only visible when ≥2 destinations and no row is being edited
    val showDragHandles = destinations.size >= 2 && activeRowIndex == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search & Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
            ) {
                // ── Filled destination rows ──────────────────────────────────
                destinations.forEachIndexed { index, dest ->
                    val isActive = activeRowIndex == index

                    if (!isActive && activeRowIndex == null) {
                        // Planning mode – reorderable input box
                        item(key = "dest_${dest.id}") {
                            ReorderableItem(reorderableState, key = "dest_${dest.id}") { isDragging ->
                                val elevation by animateDpAsState(
                                    targetValue = if (isDragging) 4.dp else 0.dp,
                                    label = "drag_elevation",
                                )
                                Surface(shadowElevation = elevation) {
                                    DestinationInputRow(
                                        value = dest.name,
                                        isActive = false,
                                        hasExistingDest = true,
                                        showDragHandle = showDragHandles,
                                        dragHandleModifier = Modifier.draggableHandle(),
                                        onTap = { viewModel.activateRow(index) },
                                        onRemove = { viewModel.removeDestination(dest.id) },
                                    )
                                }
                            }
                        }
                    } else if (isActive) {
                        // Active search field for this slot
                        item(key = "dest_${dest.id}") {
                            DestinationInputRow(
                                value = query,
                                isActive = true,
                                hasExistingDest = true,
                                focusRequester = focusRequester,
                                onValueChange = viewModel::onQueryChange,
                                onDeactivate = viewModel::deactivateRow,
                            )
                        }
                        items(results, key = { "result_${it.poi.id}" }) { result ->
                            SearchResultRow(
                                result = result,
                                onNavigate = {
                                    NavigationIntentHelper.launchSingleNavigation(
                                        context, result.poi.lat, result.poi.lng,
                                    )
                                },
                                onAddToPlan = { viewModel.addDestination(result) },
                            )
                            HorizontalDivider()
                        }
                    } else {
                        // A different row is active – show plain non-draggable input box
                        item(key = "dest_${dest.id}") {
                            DestinationInputRow(
                                value = dest.name,
                                isActive = false,
                                hasExistingDest = true,
                                onTap = { viewModel.activateRow(index) },
                                onRemove = { viewModel.removeDestination(dest.id) },
                            )
                        }
                    }
                }

                // ── Ghost row (next empty slot) ──────────────────────────────
                val isGhostActive = activeRowIndex == destinations.size
                item(key = "ghost") {
                    DestinationInputRow(
                        value = if (isGhostActive) query else "",
                        isActive = isGhostActive,
                        hasExistingDest = false,
                        placeholder = if (destinations.isEmpty()) "Add a destination…" else "Add another destination…",
                        focusRequester = if (isGhostActive) focusRequester else null,
                        onValueChange = viewModel::onQueryChange,
                        onTap = if (!isGhostActive) ({ viewModel.activateRow(destinations.size) }) else null,
                    )
                }
                if (isGhostActive) {
                    items(results, key = { "result_${it.poi.id}" }) { result ->
                        SearchResultRow(
                            result = result,
                            onNavigate = {
                                NavigationIntentHelper.launchSingleNavigation(
                                    context, result.poi.lat, result.poi.lng,
                                )
                            },
                            onAddToPlan = { viewModel.addDestination(result) },
                        )
                        HorizontalDivider()
                    }
                }
            }

            // ── Action buttons ───────────────────────────────────────────
            if (destinations.isNotEmpty() && activeRowIndex == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { NavigationIntentHelper.launchNavigation(context, destinations) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Navigate")
                    }
                    OutlinedButton(
                        onClick = {
                            planNameInput = "Plan — ${SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(Date())}"
                            showSavePlanDialog = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Create Plan")
                    }
                }
            }
        }
    }

    if (showSavePlanDialog) {
        AlertDialog(
            onDismissRequest = { showSavePlanDialog = false },
            title = { Text("Save plan") },
            text = {
                OutlinedTextField(
                    value = planNameInput,
                    onValueChange = { planNameInput = it },
                    label = { Text("Plan name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSavePlanDialog = false
                        viewModel.savePlan(planNameInput.trim().ifEmpty { "Plan" })
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePlanDialog = false }) { Text("Cancel") }
            },
        )
    }

    LaunchedEffect(activeRowIndex) {
        if (activeRowIndex != null) {
            kotlinx.coroutines.delay(50)
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.activateRow(viewModel.destinations.value.size)
    }
}

/**
 * A single destination slot rendered as an OutlinedTextField.
 * When [isActive], the field is editable and receives focus.
 * When inactive, it is read-only with a transparent tap layer so the user can tap to re-search.
 * The remove button lives outside the field to avoid being intercepted by the tap overlay.
 */
@Composable
private fun DestinationInputRow(
    value: String,
    isActive: Boolean,
    hasExistingDest: Boolean,
    placeholder: String = "Search for a destination…",
    onValueChange: (String) -> Unit = {},
    onTap: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onDeactivate: (() -> Unit)? = null,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDragHandle) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = dragHandleModifier,
            )
            Spacer(Modifier.width(4.dp))
        }

        val trailingIcon: (@Composable () -> Unit)? = when {
            isActive && value.isNotEmpty() -> {
                { IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, "Clear") } }
            }
            isActive && value.isEmpty() && hasExistingDest -> {
                { IconButton(onClick = { onDeactivate?.invoke() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel") } }
            }
            else -> null
        }

        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = trailingIcon,
                readOnly = !isActive,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            )
            // Transparent overlay on read-only fields so taps route to our handler
            if (!isActive && onTap != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(onTap) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.pressed }) {
                                        event.changes.forEach { it.consume() }
                                        onTap()
                                    }
                                }
                            }
                        },
                )
            }
        }

        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove destination")
            }
        }
    }
}

