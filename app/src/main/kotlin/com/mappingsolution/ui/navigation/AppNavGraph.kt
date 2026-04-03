package com.mappingsolution.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mappingsolution.ui.library.GroupFormScreen
import com.mappingsolution.ui.library.GroupFormViewModel
import com.mappingsolution.ui.library.IconPickerScreen
import com.mappingsolution.ui.library.LibraryScreen
import com.mappingsolution.ui.main.MainScreen
import com.mappingsolution.ui.poi.PoiDetailScreen
import com.mappingsolution.ui.poi.PoiFormScreen

private const val ROUTE_MAIN = "main"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_GROUP_FORM = "group_form"
private const val ROUTE_GROUP_FORM_EDIT = "group_form/{groupId}"
private const val ROUTE_ICON_PICKER = "icon_picker"
private const val ROUTE_POI_FORM_NEW = "poi_form_new?lat={lat}&lng={lng}"
private const val ROUTE_POI_DETAIL = "poi_detail/{poiId}"
private const val ROUTE_POI_FORM_EDIT = "poi_form_edit/{poiId}"

private const val KEY_GROUP_ID = "groupId"
private const val KEY_POI_ID = "poiId"
private const val KEY_LAT = "lat"
private const val KEY_LNG = "lng"
private const val KEY_SELECTED_ICON = "selected_icon"
private const val KEY_CURRENT_ICON = "current_icon"

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_MAIN) {

        composable(ROUTE_MAIN) {
            MainScreen(
                onOpenLibrary = { navController.navigate(ROUTE_LIBRARY) },
                onAddPoi = { lat, lng ->
                    navController.navigate("poi_form_new?lat=$lat&lng=$lng")
                },
                onPoiTapped = { poiId ->
                    navController.navigate("poi_detail/$poiId")
                },
            )
        }

        composable(
            route = ROUTE_POI_FORM_NEW,
            arguments = listOf(
                navArgument(KEY_LAT) { type = NavType.StringType; defaultValue = "0.0" },
                navArgument(KEY_LNG) { type = NavType.StringType; defaultValue = "0.0" },
            ),
        ) {
            PoiFormScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateGroup = { navController.navigate(ROUTE_GROUP_FORM) },
            )
        }

        composable(
            route = ROUTE_POI_DETAIL,
            arguments = listOf(navArgument(KEY_POI_ID) { type = NavType.LongType }),
        ) {
            PoiDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { poiId ->
                    navController.navigate("poi_form_edit/$poiId")
                },
            )
        }

        composable(
            route = ROUTE_POI_FORM_EDIT,
            arguments = listOf(navArgument(KEY_POI_ID) { type = NavType.LongType }),
        ) {
            PoiFormScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateGroup = { navController.navigate(ROUTE_GROUP_FORM) },
            )
        }

        composable(ROUTE_LIBRARY) {
            LibraryScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateGroup = { navController.navigate(ROUTE_GROUP_FORM) },
                onEditGroup = { groupId -> navController.navigate("group_form/$groupId") },
            )
        }

        composable(ROUTE_GROUP_FORM) { backStackEntry ->
            val vm: GroupFormViewModel = hiltViewModel(backStackEntry)

            val selectedIcon = backStackEntry.savedStateHandle
                .getStateFlow(KEY_SELECTED_ICON, "")
                .collectAsState()
            LaunchedEffect(selectedIcon.value) {
                if (selectedIcon.value.isNotEmpty()) {
                    vm.onIconChange(selectedIcon.value)
                    backStackEntry.savedStateHandle.remove<String>(KEY_SELECTED_ICON)
                }
            }

            GroupFormScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIconPicker = { currentKey ->
                    backStackEntry.savedStateHandle[KEY_CURRENT_ICON] = currentKey
                    navController.navigate(ROUTE_ICON_PICKER)
                },
                viewModel = vm,
            )
        }

        composable(
            route = ROUTE_GROUP_FORM_EDIT,
            arguments = listOf(navArgument(KEY_GROUP_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val vm: GroupFormViewModel = hiltViewModel(backStackEntry)

            val selectedIcon = backStackEntry.savedStateHandle
                .getStateFlow(KEY_SELECTED_ICON, "")
                .collectAsState()
            LaunchedEffect(selectedIcon.value) {
                if (selectedIcon.value.isNotEmpty()) {
                    vm.onIconChange(selectedIcon.value)
                    backStackEntry.savedStateHandle.remove<String>(KEY_SELECTED_ICON)
                }
            }

            GroupFormScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToIconPicker = { currentKey ->
                    backStackEntry.savedStateHandle[KEY_CURRENT_ICON] = currentKey
                    navController.navigate(ROUTE_ICON_PICKER)
                },
                viewModel = vm,
            )
        }

        composable(ROUTE_ICON_PICKER) {
            val formEntry = navController.previousBackStackEntry
            val currentIcon = formEntry?.savedStateHandle?.get<String>(KEY_CURRENT_ICON) ?: "place"

            IconPickerScreen(
                currentIconKey = currentIcon,
                onIconSelected = { key ->
                    formEntry?.savedStateHandle?.set(KEY_SELECTED_ICON, key)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
