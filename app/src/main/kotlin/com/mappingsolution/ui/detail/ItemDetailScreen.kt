package com.mappingsolution.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.ui.poi.media.PoiMediaGallery
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditPoi: (poiId: String) -> Unit,
    onNavigateToEditRoute: (routeId: String) -> Unit,
    onOpenMediaPreview: (poiId: String, index: Int, paths: List<String>) -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val title = when (val item = state.item) {
        is DetailItem.PoiDetail -> item.poi.name
        is DetailItem.RouteDetail -> item.route.name
        null -> if (state.isLoading) "…" else "Not found"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        when (val item = state.item) {
            null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("Not found", style = MaterialTheme.typography.bodyLarge) }

            is DetailItem.PoiDetail -> PoiDetailContent(
                item = item,
                modifier = Modifier.padding(padding),
                isReadOnly = item.isReadOnly,
                onNavigateBack = onNavigateBack,
                onNavigateToEdit = onNavigateToEditPoi,
                onOpenMediaPreview = onOpenMediaPreview,
                onDeleteClick = { viewModel.deletePoi(it) },
                onRemoveMedia = { viewModel.removePoiMediaItem(it) },
                context = context,
            )

            is DetailItem.RouteDetail -> RouteDetailContent(
                item = item,
                modifier = Modifier.padding(padding),
                onNavigateBack = onNavigateBack,
                onNavigateToEdit = onNavigateToEditRoute,
                onDeleteClick = { viewModel.deleteRoute(it) },
                context = context,
            )
        }
    }
}

@Composable
private fun PoiDetailContent(
    item: DetailItem.PoiDetail,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (poiId: String) -> Unit,
    onOpenMediaPreview: (poiId: String, index: Int, paths: List<String>) -> Unit,
    onDeleteClick: (onDeleted: () -> Unit) -> Unit,
    onRemoveMedia: (index: Int) -> Unit,
    context: android.content.Context,
) {
    val poi = item.poi
    var lastDeleteClickTime by remember { mutableStateOf(0L) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LabeledField("Name", poi.name)

            if (!poi.description.isNullOrBlank()) {
                LabeledField("Description", poi.description)
            }

            LabeledField("Group", item.group?.name ?: "No group")

            LabeledField("Location", "%.6f, %.6f".format(poi.lat, poi.lng))

            LabeledField(
                label = "Added",
                value = formatDate(poi.createdAt),
            )

            if (item.mediaPaths.isNotEmpty()) {
                val mediaItems = item.mediaPaths.mapIndexed { index, path ->
                    com.mappingsolution.data.model.MediaUtils.createMediaItem(path, index)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Media",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PoiMediaGallery(
                        mediaItems = mediaItems,
                        onItemClick = { index -> onOpenMediaPreview(poi.id, index, item.mediaPaths) },
                        onRemoveItem = { index -> onRemoveMedia(index) },
                        allowRemove = !isReadOnly,
                    )
                }
            }
        }

        DetailBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onEditClick = { onNavigateToEdit(poi.id) },
            editLabel = "Edit POI",
            deleteLabel = "Remove POI",
            isReadOnly = isReadOnly,
            onDeleteClick = {
                val now = System.currentTimeMillis()
                if (now - lastDeleteClickTime < 2000) {
                    onDeleteClick {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            onNavigateBack()
                        }, 100)
                    }
                } else {
                    lastDeleteClickTime = now
                    android.widget.Toast.makeText(
                        context, "Tap again quickly to remove POI", android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            context = context,
        )
    }
}

@Composable
private fun RouteDetailContent(
    item: DetailItem.RouteDetail,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (routeId: String) -> Unit,
    onDeleteClick: (onDeleted: () -> Unit) -> Unit,
    context: android.content.Context,
) {
    val route = item.route
    var lastDeleteClickTime by remember { mutableStateOf(0L) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LabeledField("Name", route.name)

            if (!route.description.isNullOrBlank()) {
                LabeledField("Description", route.description)
            }

            ColorField(label = "Color", colorHex = route.color)

            LabeledField("Distance", formatDistance(route.distanceMeters))

            LabeledField("Duration", formatDuration(route.durationSec))

            LabeledField("Recorded", formatDate(route.startedAt))
        }

        DetailBottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onEditClick = { onNavigateToEdit(route.id) },
            editLabel = "Edit Route",
            deleteLabel = "Remove Route",
            onDeleteClick = {
                val now = System.currentTimeMillis()
                if (now - lastDeleteClickTime < 2000) {
                    onDeleteClick {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            onNavigateBack()
                        }, 100)
                    }
                } else {
                    lastDeleteClickTime = now
                    android.widget.Toast.makeText(
                        context, "Tap again quickly to remove route", android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            context = context,
        )
    }
}

@Composable
private fun DetailBottomBar(
    modifier: Modifier = Modifier,
    onEditClick: () -> Unit,
    editLabel: String,
    deleteLabel: String,
    isReadOnly: Boolean = false,
    onDeleteClick: () -> Unit,
    context: android.content.Context? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column {
            Button(
                onClick = {
                    if (isReadOnly) {
                        android.widget.Toast.makeText(
                            context, "Cannot edit this POI", android.widget.Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onEditClick()
                    }
                },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(52.dp),
                colors = if (isReadOnly) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(editLabel)
            }
            if (!isReadOnly) {
                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text(deleteLabel)
                }
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ColorField(label: String, colorHex: String) {
    val color = runCatching {
        // colorHex may be #AARRGGBB or #RRGGBB
        val hex = if (colorHex.length == 9 && colorHex.startsWith("#")) {
            "#${colorHex.substring(3)}"
        } else colorHex
        Color(android.graphics.Color.parseColor(hex))
    }.getOrElse { MaterialTheme.colorScheme.primary }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(text = colorHex, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatDistance(meters: Double): String = when {
    meters < 1000 -> "${meters.roundToInt()} m"
    else -> "${"%.1f".format(meters / 1000)} km"
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
