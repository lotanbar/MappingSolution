package com.mappingsolution.ui.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BottomActionPanel(
    onAddPoi: () -> Unit,
    onRecordRoute: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAddPoi, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.AddLocation,
                contentDescription = "Add POI at current location",
                modifier = Modifier.size(32.dp)
            )
        }
        IconButton(onClick = onRecordRoute, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.RadioButtonChecked,
                contentDescription = "Record a route",
                modifier = Modifier.size(32.dp)
            )
        }
        IconButton(onClick = onOpenLibrary, modifier = Modifier.size(56.dp)) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Open library",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
