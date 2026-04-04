package com.mappingsolution.ui.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mappingsolution.data.recording.RecordingState
import kotlinx.coroutines.delay

/** Collapsible pane content shown below the main action row during an active recording. */
@Composable
fun RecordingPane(
    state: RecordingState.Active,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }

    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatElapsed(state.elapsedMs(nowMs)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = formatDistance(state.distanceMeters),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        if (state.isPaused) {
            IconButton(onClick = onResume, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume recording",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            IconButton(onClick = onPause, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause recording",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        IconButton(onClick = onStop, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop recording",
                tint = Color.Red,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

internal fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

internal fun formatDistance(meters: Double): String =
    if (meters >= 1000) "%.2f km".format(meters / 1000) else "%.0f m".format(meters)
