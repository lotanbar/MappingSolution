package com.mappingsolution.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.ui.main.components.BottomActionPanel
import com.mappingsolution.ui.main.components.MapComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val scaffoldState = rememberBottomSheetScaffoldState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val peekHeight = screenHeightDp * 0.20f

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        containerColor = MaterialTheme.colorScheme.background,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            BottomActionPanel(
                onAddPoi = { /* commit 3 */ },
                onRecordRoute = { /* commit 4 */ },
                onOpenLibrary = { /* commit 6 */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        MapComponent(modifier = Modifier.fillMaxSize())
    }
}
