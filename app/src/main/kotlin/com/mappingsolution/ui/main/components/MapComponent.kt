package com.mappingsolution.ui.main.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mappingsolution.BuildConfig
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

private const val MAPTILER_STYLE_URL =
    "https://api.maptiler.com/maps/hybrid/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

@Composable
fun MapComponent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    MapLibre.getInstance(context)

    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycle) {
        mapView.onStart()
        onDispose { mapView.onStop() }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map: MapLibreMap ->
                map.setStyle(Style.Builder().fromUri(MAPTILER_STYLE_URL))
            }
        }
    )
}
