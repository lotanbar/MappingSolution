package com.mappingsolution.ui.poi

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.data.model.MediaItem
import com.mappingsolution.data.model.MediaType
import com.mappingsolution.ui.poi.media.PoiMediaGallery
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (poiId: Long) -> Unit,
    onOpenMediaPreview: (poiId: Long, index: Int, paths: List<String>) -> Unit,
    viewModel: PoiDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.poi?.name ?: "POI") },
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

        val poi = state.poi
        if (poi == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("POI not found", style = MaterialTheme.typography.bodyLarge) }
            return@Scaffold
        }

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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

                LabeledField("Group", state.group?.name ?: "No group")

                LabeledField("Location", "%.6f, %.6f".format(poi.lat, poi.lng))

                LabeledField(
                    label = "Added",
                    value = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date(poi.createdAt)),
                )

                if (state.mediaPaths.isNotEmpty()) {
                    val mediaItems = state.mediaPaths.mapIndexed { index, path ->
                        com.mappingsolution.data.model.MediaUtils.createMediaItem(path, index)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Media",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        PoiMediaGallery(
                            mediaItems = mediaItems,
                            onItemClick = { index -> onOpenMediaPreview(poi.id, index, state.mediaPaths) },
                            onRemoveItem = { index -> viewModel.removeMediaItem(index) }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                Column {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var lastDeleteClickTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }
                    Button(
                        onClick = { onNavigateToEdit(poi.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(52.dp),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit")
                    }

                    Button(
                        onClick = {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastDeleteClickTime < 2000) {
                                viewModel.deletePoi {
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        onNavigateBack()
                                    }, 100)
                                }
                            } else {
                                lastDeleteClickTime = currentTime
                                android.widget.Toast.makeText(context, "Tap again quickly to remove POI", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(52.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Remove POI")
                    }
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
