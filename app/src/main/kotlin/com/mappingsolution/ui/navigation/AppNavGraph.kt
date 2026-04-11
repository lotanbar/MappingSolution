package com.mappingsolution.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mappingsolution.service.RecordingService
import com.mappingsolution.ui.library.GroupFormScreen
import com.mappingsolution.ui.library.GroupFormViewModel
import com.mappingsolution.ui.library.IconPickerScreen
import com.mappingsolution.ui.library.LibraryScreen
import com.mappingsolution.ui.main.MainScreen
import com.mappingsolution.ui.detail.ItemDetailScreen
import com.mappingsolution.ui.poi.PoiFormScreen
import com.mappingsolution.ui.poi.media.MediaPreviewScreen
import com.mappingsolution.ui.recording.RouteFinalizeScreen

private const val ROUTE_MAIN = "main"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_GROUP_FORM = "group_form"
private const val ROUTE_GROUP_FORM_EDIT = "group_form/{groupId}"
private const val ROUTE_ICON_PICKER = "icon_picker"
private const val ROUTE_POI_FORM_NEW = "poi_form_new?lat={lat}&lng={lng}"
private const val ROUTE_ITEM_DETAIL = "item_detail/{type}/{id}"
private const val ROUTE_POI_MEDIA_PREVIEW = "poi_media_preview/{poiId}?startIndex={startIndex}"
private const val ROUTE_POI_FORM_EDIT = "poi_form_edit/{poiId}"
private const val ROUTE_ROUTE_FINALIZE = "route_finalize/{routeId}"
/** Edit a saved route from the Library (no discard guard). */
private const val ROUTE_ROUTE_EDIT = "route_edit/{routeId}"

private const val KEY_GROUP_ID = "groupId"
private const val KEY_POI_ID = "poiId"
private const val KEY_ROUTE_ID = "routeId"
private const val KEY_DETAIL_TYPE = "type"
private const val KEY_DETAIL_ID = "id"
private const val KEY_START_INDEX = "startIndex"
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
                onAddPoi = { lat, lng -> navController.navigate("poi_form_new?lat=$lat&lng=$lng") },
                onPoiTapped = { poiId -> navController.navigate("item_detail/poi/$poiId") },
                onRouteTapped = { routeId -> navController.navigate("item_detail/route/$routeId") },
                onGooglePlaceTapped = { placeId -> navController.navigate("item_detail/google_place/$placeId") },
                onOsmPoiTapped = { osmId -> navController.navigate("item_detail/osm_poi/$osmId") },
                onNavigateToFinalize = { routeId -> navController.navigate("route_finalize/$routeId") },
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
                onNavigateToMediaPreview = { poiId, index, paths ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("media_paths", paths)
                    navController.navigate("poi_media_preview/$poiId?startIndex=$index")
                },
                onCreateGroup = { navController.navigate(ROUTE_GROUP_FORM) },
            )
        }

        composable(
            route = ROUTE_ITEM_DETAIL,
            arguments = listOf(
                navArgument(KEY_DETAIL_TYPE) { type = NavType.StringType },
                navArgument(KEY_DETAIL_ID) { type = NavType.StringType },
            ),
        ) {
            ItemDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditPoi = { poiId -> navController.navigate("poi_form_edit/$poiId") },
                onNavigateToEditRoute = { routeId -> navController.navigate("route_edit/$routeId") },
                onOpenMediaPreview = { poiId, index, paths ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("media_paths", paths)
                    navController.navigate("poi_media_preview/$poiId?startIndex=$index")
                },
            )
        }

        composable(
            route = ROUTE_POI_MEDIA_PREVIEW,
            arguments = listOf(
                navArgument(KEY_POI_ID) { type = NavType.StringType },
                navArgument(KEY_START_INDEX) { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { backStackEntry ->
            val startIndex = backStackEntry.arguments?.getInt(KEY_START_INDEX) ?: 0
            val paths = navController.previousBackStackEntry?.savedStateHandle?.get<List<String>>("media_paths") ?: emptyList()
            MediaPreviewScreen(paths = paths, startIndex = startIndex)
        }

        composable(
            route = ROUTE_POI_FORM_EDIT,
            arguments = listOf(navArgument(KEY_POI_ID) { type = NavType.StringType }),
        ) {
            PoiFormScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMediaPreview = { poiId, index, paths ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("media_paths", paths)
                    navController.navigate("poi_media_preview/$poiId?startIndex=$index")
                },
                onCreateGroup = { navController.navigate(ROUTE_GROUP_FORM) },
            )
        }

        composable(ROUTE_LIBRARY) {
            val context = LocalContext.current
            LibraryScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateGroup = { navController.navigate(ROUTE_GROUP_FORM) },
                onEditGroup = { groupId -> navController.navigate("group_form/$groupId") },
                onEditPoi = { poiId -> navController.navigate("poi_form_edit/$poiId") },
                onEditRoute = { routeId -> navController.navigate("route_edit/$routeId") },
                onContinueRecording = { routeId ->
                    context.startService(RecordingService.resumeIncompleteIntent(context, routeId))
                    navController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_MAIN) { inclusive = false }
                    }
                },
            )
        }

        composable(ROUTE_GROUP_FORM) { backStackEntry ->
            val vm: GroupFormViewModel = hiltViewModel(backStackEntry)
            val selectedIcon = backStackEntry.savedStateHandle
                .getStateFlow(KEY_SELECTED_ICON, "").collectAsState()
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
            arguments = listOf(navArgument(KEY_GROUP_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val vm: GroupFormViewModel = hiltViewModel(backStackEntry)
            val selectedIcon = backStackEntry.savedStateHandle
                .getStateFlow(KEY_SELECTED_ICON, "").collectAsState()
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

        composable(
            route = ROUTE_ROUTE_FINALIZE,
            arguments = listOf(navArgument(KEY_ROUTE_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString(KEY_ROUTE_ID) ?: return@composable
            RouteFinalizeScreen(
                routeId = routeId,
                onDone = { navController.popBackStack() },
            )
        }

        composable(
            route = ROUTE_ROUTE_EDIT,
            arguments = listOf(navArgument(KEY_ROUTE_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString(KEY_ROUTE_ID) ?: return@composable
            RouteFinalizeScreen(
                routeId = routeId,
                isLibraryEdit = true,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
