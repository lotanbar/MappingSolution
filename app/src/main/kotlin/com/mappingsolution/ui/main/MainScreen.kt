package com.mappingsolution.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.ui.main.components.BottomActionPanel
import com.mappingsolution.ui.main.components.MapComponent

@Composable
fun MainScreen(
    onOpenLibrary: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = maxHeight * 0.85f
        val panelHeight = maxHeight * 0.15f
        Column(modifier = Modifier.fillMaxSize()) {
            MapComponent(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeight)
            )
            BottomActionPanel(
                onAddPoi = { /* commit 3 */ },
                onRecordRoute = { /* commit 4 */ },
                onOpenLibrary = onOpenLibrary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
