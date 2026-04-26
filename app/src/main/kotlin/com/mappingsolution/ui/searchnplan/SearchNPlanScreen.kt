package com.mappingsolution.ui.searchnplan

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.ui.searchnplan.components.DestinationRow
import com.mappingsolution.ui.searchnplan.components.SearchResultRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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

    val focusRequester = remember { FocusRequester() }

    val lazyListState = rememberLazyListState()

    // Keep latest sizes in stable State refs so the reorderable callback never captures stale values.
    val resultsSize = remember { androidx.compose.runtime.derivedStateOf { results.size } }
    val destinationsSize = remember { androidx.compose.runtime.derivedStateOf { destinations.size } }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Layout: [results...] [dest_header?] [dest_0..dest_n] [ghost_row?]
        val destStart = resultsSize.value + if (destinationsSize.value > 0) 1 else 0
        val localFrom = from.index - destStart
        val localTo = to.index - destStart
        if (localFrom in 0 until destinationsSize.value && localTo in 0 until destinationsSize.value) {
            viewModel.moveDestination(localFrom, localTo)
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Type to search…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
            ) {
                // ── Search results ──────────────────────────────────────────
                items(results, key = { "result_${it.poi.id}" }) { result ->
                    SearchResultRow(
                        result = result,
                        onNavigate = {
                            NavigationIntentHelper.launchSingleNavigation(
                                context, result.poi.lat, result.poi.lng,
                            )
                        },
                        onAddToPlan = {
                            viewModel.addDestination(result)
                            focusRequester.requestFocus()
                        },
                    )
                    HorizontalDivider()
                }

                // ── Destinations section ────────────────────────────────────
                if (destinations.isNotEmpty()) {
                    item(key = "dest_header") {
                        Text(
                            text = "Destinations",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    items(destinations, key = { "dest_${it.id}" }) { dest ->
                        ReorderableItem(reorderableState, key = "dest_${dest.id}") { isDragging ->
                            val elevation by animateDpAsState(
                                targetValue = if (isDragging) 4.dp else 0.dp,
                                label = "drag_elevation",
                            )
                            Surface(shadowElevation = elevation) {
                                DestinationRow(
                                    destination = dest,
                                    onRemove = { viewModel.removeDestination(dest.id) },
                                    dragHandleModifier = Modifier.draggableHandle(),
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    // Ghost "Add another destination" row
                    item(key = "ghost_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { focusRequester.requestFocus() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Add another destination",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // ── Navigate button (shown when >= 1 destination) ───────────────
            if (destinations.isNotEmpty()) {
                Button(
                    onClick = { NavigationIntentHelper.launchNavigation(context, destinations) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Navigate")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

