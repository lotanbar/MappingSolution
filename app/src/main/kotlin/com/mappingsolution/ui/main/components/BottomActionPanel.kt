package com.mappingsolution.ui.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mappingsolution.data.recording.RecordingState

@Composable
fun BottomActionPanel(
    onAddPoi: () -> Unit,
    onRecordRoute: () -> Unit,
    onOpenLibrary: () -> Unit,
    onFlyToLocation: () -> Unit = {},
    recordingState: RecordingState = RecordingState.Idle,
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onFlyToLocation, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = "Fly to current location",
                modifier = Modifier.size(32.dp),
                tint = Color.White,
            )
        }

        IconButton(onClick = onAddPoi, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.AddLocation,
                contentDescription = "Add POI at current location",
                modifier = Modifier.size(32.dp),
                tint = Color.White,
            )
        }

        if (recordingState is RecordingState.Active) {
            RecordingPane(
                state = recordingState,
                onPause = onPauseRecording,
                onResume = onResumeRecording,
                onStop = onStopRecording,
                modifier = Modifier.weight(1f),
            )
        } else {
            IconButton(onClick = onRecordRoute, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.RadioButtonChecked,
                    contentDescription = "Record a route",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White,
                )
            }
        }

        IconButton(onClick = onOpenLibrary, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Open library",
                modifier = Modifier.size(32.dp),
                tint = Color.White,
            )
        }
    }
}
