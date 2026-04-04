package com.mappingsolution.ui.main.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.gson.JsonObject
import com.mappingsolution.BuildConfig
import com.mappingsolution.createPinBitmap
import com.mappingsolution.data.db.entity.GroupEntity
import com.mappingsolution.data.db.entity.PoiEntity
import com.mappingsolution.data.recording.RecordingPoint
import com.mappingsolution.ui.common.IconCatalog
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private fun styleUrl() =
    "https://api.maptiler.com/maps/hybrid/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

private fun createPoiPin(
    colorHex: String,
    painter: Painter,
    density: Density,
    layoutDirection: LayoutDirection
): Bitmap {
    val width = 75
    val height = 101
    val bitmap = createPinBitmap(colorHex, width, height)
    val androidCanvas = android.graphics.Canvas(bitmap)
    val composeCanvas = androidx.compose.ui.graphics.Canvas(androidCanvas)
    val drawScope = CanvasDrawScope()
    val iconSize = width * 0.55f
    val offset = (width - iconSize) / 2f
    
    val pinColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        Color.Blue
    }

    drawScope.draw(density, layoutDirection, composeCanvas, Size(width.toFloat(), height.toFloat())) {
        withTransform({
            translate(offset, (width - iconSize) / 2f) 
        }) {
            with(painter) {
                draw(
                    size = Size(iconSize, iconSize),
                    colorFilter = ColorFilter.tint(pinColor)
                )
            }
        }
    }
    return bitmap
}

@Composable
fun MapComponent(
    pois: List<PoiEntity> = emptyList(),
    groups: List<GroupEntity> = emptyList(),
    liveRoutePoints: List<RecordingPoint> = emptyList(),
    flyToLocation: Pair<Double, Double>? = null,
    onPoiTapped: (Long) -> Unit = {},
    onMapError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val onPoiTappedRef = rememberUpdatedState(onPoiTapped)

    MapLibre.getInstance(context)

    val mapView = remember { MapView(context) }
    val mapState = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleReady = remember { mutableStateOf(false) }
    val onMapErrorRef = rememberUpdatedState(onMapError)

    // Generate painters for all needed icons
    val iconKeys = remember(groups) { groups.map { it.iconKey }.toSet() + "place" }
    val painters = iconKeys.associateWith { rememberVectorPainter(IconCatalog.iconVector(it)) }

    // Generate bitmaps for each group + default
    val groupBitmaps = remember(groups, painters) {
        val bitmaps = mutableMapOf<String, Bitmap>()
        groups.forEach { group ->
            val painter = painters[group.iconKey] ?: painters["place"]!!
            bitmaps[group.id.toString()] = createPoiPin(group.color, painter, density, layoutDirection)
        }
        bitmaps["default"] = createPoiPin("#2196F3", painters["place"]!!, density, layoutDirection)
        bitmaps
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Update style images when groupBitmaps change
    LaunchedEffect(styleReady.value, groupBitmaps) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        
        groupBitmaps.forEach { (id, bitmap) ->
            style.addImage("pin-$id", bitmap)
        }
    }

    // Re-render POIs whenever data or style readiness changes
    LaunchedEffect(pois, groups, styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val source = style.getSource("poi-source") as? GeoJsonSource ?: return@LaunchedEffect

        val features = pois.filter { it.isVisible }.map { poi ->
            val iconId = "pin-${poi.groupId ?: "default"}"
            val props = JsonObject().apply {
                addProperty("poiId", poi.id)
                addProperty("icon-id", iconId)
                addProperty("name", poi.name)
            }
            Feature.fromGeometry(Point.fromLngLat(poi.lng, poi.lat), props)
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    // Fly to requested location
    LaunchedEffect(flyToLocation) {
        val (lat, lng) = flyToLocation ?: return@LaunchedEffect
        val map = mapState.value ?: return@LaunchedEffect
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17.0))
    }

    // Re-render live route polyline whenever points change
    LaunchedEffect(liveRoutePoints, styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val lineSource = style.getSource("live-route-source") as? GeoJsonSource ?: return@LaunchedEffect
        if (liveRoutePoints.size >= 2) {
            val pts = liveRoutePoints.map { Point.fromLngLat(it.lng, it.lat) }
            lineSource.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(pts)))
        } else {
            lineSource.setGeoJson(FeatureCollection.fromFeatures(emptyList<Feature>()))
        }
    }

    AndroidView(
        factory = {
            mapView.addOnDidFailLoadingMapListener {
                onMapErrorRef.value("Map failed to load. Check your API key or connection.")
            }
            mapView.getMapAsync { map ->
                mapState.value = map
                map.setStyle(Style.Builder().fromUri(styleUrl())) { style ->
                    style.addSource(
                        GeoJsonSource("poi-source", FeatureCollection.fromFeatures(emptyList<Feature>()))
                    )
                    style.addSource(GeoJsonSource("live-route-source"))
                    style.addLayer(
                        LineLayer("live-route-line", "live-route-source").withProperties(
                            PropertyFactory.lineColor("#FF5722"),
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        )
                    )
                    style.addLayer(
                        SymbolLayer("poi-symbols", "poi-source").withProperties(
                            PropertyFactory.iconImage(Expression.get("icon-id")),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                        )
                    )
                    map.addOnMapClickListener { latLng ->
                        val pt = map.projection.toScreenLocation(latLng)
                        val hit = map.queryRenderedFeatures(
                            RectF(pt.x - 20f, pt.y - 20f, pt.x + 20f, pt.y + 20f),
                            "poi-symbols",
                        )
                        if (hit.isNotEmpty()) {
                            val poiId = hit[0].getNumberProperty("poiId")?.toLong()
                            if (poiId != null) {
                                onPoiTappedRef.value(poiId)
                                return@addOnMapClickListener true
                            }
                        }
                        false
                    }
                    styleReady.value = true
                }
            }
            mapView
        },
        modifier = modifier,
    )
}
