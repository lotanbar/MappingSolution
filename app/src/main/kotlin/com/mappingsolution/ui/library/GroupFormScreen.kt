package com.mappingsolution.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.ui.common.IconCatalog
import com.mappingsolution.ui.library.components.ColorPickerDialog
import com.mappingsolution.ui.library.components.parseHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupFormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToIconPicker: (currentIconKey: String) -> Unit,
    viewModel: GroupFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        ColorPickerDialog(
            initialHex = state.color,
            onConfirm = { hex ->
                viewModel.onColorChange(hex)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit Group" else "New Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Scrollable fields ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Name
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    isError = state.nameError != null,
                    supportingText = state.nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Description
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description (optional)") },
                    isError = state.descriptionError != null,
                    supportingText = state.descriptionError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Icon selector
                Column {
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.iconError != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    OutlinedButton(
                        onClick = { onNavigateToIconPicker(state.iconKey) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = IconCatalog.iconVector(state.iconKey),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(state.iconKey.replace('_', ' ').replaceFirstChar { it.uppercase() })
                        Spacer(Modifier.weight(1f))
                        Text("Change", style = MaterialTheme.typography.labelMedium)
                    }
                    if (state.iconError != null) {
                        Text(
                            text = state.iconError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        )
                    }
                }

                // Color selector
                Column {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.colorError != null) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    val currentColor = parseHex(state.color)
                    OutlinedButton(
                        onClick = { showColorPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .drawBehind { drawCircle(currentColor) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(state.color)
                        Spacer(Modifier.weight(1f))
                        Text("Change", style = MaterialTheme.typography.labelMedium)
                    }
                    if (state.colorError != null) {
                        Text(
                            text = state.colorError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        )
                    }
                }
            }

            // ── Pinned bottom button ─────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Button(
                    onClick = { viewModel.save { onNavigateBack() } },
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (viewModel.isEditing) "Save" else "Create")
                    }
                }
            }
        }
    }
}
