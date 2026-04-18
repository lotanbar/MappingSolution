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
import com.mappingsolution.PoiSource
import com.mappingsolution.createCircleIcon
import com.mappingsolution.createPinBitmap
import com.mappingsolution.data.model.Group
import com.mappingsolution.data.model.Poi
import com.mappingsolution.data.model.Route
import com.mappingsolution.data.model.RoutePoint
import com.mappingsolution.data.recording.RecordingPoint
import com.mappingsolution.ui.common.IconCatalog
import com.mappingsolution.data.prefs.ViewportPreference
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
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

private fun createPoiCircle(
    iconKey: String,
    source: PoiSource,
    painter: Painter,
    density: Density,
    layoutDirection: LayoutDirection,
    size: Int = 80,
): Bitmap {
    val bitmap = createCircleIcon(iconKey, source, size = size)
    val androidCanvas = android.graphics.Canvas(bitmap)
    val composeCanvas = androidx.compose.ui.graphics.Canvas(androidCanvas)
    val drawScope = CanvasDrawScope()
    val iconSize = size * 0.55f
    val offset = (size - iconSize) / 2f

    drawScope.draw(density, layoutDirection, composeCanvas, Size(size.toFloat(), size.toFloat())) {
        withTransform({ translate(offset, offset) }) {
            with(painter) {
                draw(
                    size = Size(iconSize, iconSize),
                    colorFilter = ColorFilter.tint(Color.White),
                )
            }
        }
    }
    return bitmap
}

@Composable
fun MapComponent(
    pois: List<Poi> = emptyList(),
    groups: List<Group> = emptyList(),
    routes: List<Route> = emptyList(),
    routePoints: Map<String, List<RoutePoint>> = emptyMap(),
    googlePlaces: List<Poi> = emptyList(),
    osmPois: List<Poi> = emptyList(),
    bulkPois: List<Poi> = emptyList(),
    liveRoutePoints: List<RecordingPoint> = emptyList(),
    liveRouteColor: String = "#FFFF5722",
    flyToLocation: Pair<Double, Double>? = null,
    initialCamera: ViewportPreference.SavedCamera? = null,
    onCameraIdle: (lat: Double, lng: Double, zoom: Double, bearing: Double, tilt: Double) -> Unit = { _, _, _, _, _ -> },
    onBoundsChanged: (north: Double, south: Double, east: Double, west: Double, lat: Double, lng: Double, zoom: Double, bearing: Double, tilt: Double) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onPoiTapped: (String) -> Unit = {},
    onRouteTapped: (String) -> Unit = {},
    onGooglePlaceTapped: (String) -> Unit = {},
    onOsmPoiTapped: (String) -> Unit = {},
    onBulkPoiTapped: (String) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {},
    onMapDisposed: () -> Unit = {},
    onMapError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val onPoiTappedRef = rememberUpdatedState(onPoiTapped)
    val onRouteTappedRef = rememberUpdatedState(onRouteTapped)
    val onGooglePlaceTappedRef = rememberUpdatedState(onGooglePlaceTapped)
    val onOsmPoiTappedRef = rememberUpdatedState(onOsmPoiTapped)
    val onBulkPoiTappedRef = rememberUpdatedState(onBulkPoiTapped)
    val onCameraIdleRef = rememberUpdatedState(onCameraIdle)
    val onBoundsChangedRef = rememberUpdatedState(onBoundsChanged)

    MapLibre.getInstance(context)

    val mapView = remember { MapView(context) }
    val mapState = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleReady = remember { mutableStateOf(false) }
    val onMapErrorRef = rememberUpdatedState(onMapError)

    // Pre-create painters for ALL catalog icons (fixed set — composable safe)
    val allIconKeys = remember { IconCatalog.categories.flatMap { it.icons }.map { it.key } }
    val allPainters = allIconKeys.associateWith { rememberVectorPainter(IconCatalog.iconVector(it)) }
    val placePainterFallback = allPainters["place"] ?: rememberVectorPainter(IconCatalog.iconVector("place"))

    // Painters for group icons (subset of allPainters, kept for groupBitmaps)
    val painters = allPainters

    // Generate bitmaps for each group + default
    val groupBitmaps = remember(groups, painters) {
        val bitmaps = mutableMapOf<String, Bitmap>()
        groups.forEach { group ->
            val painter = painters[group.iconKey] ?: placePainterFallback
            bitmaps[group.id] = createPoiPin(group.color, painter, density, layoutDirection)
        }
        bitmaps["default"] = createPoiPin("#2196F3", placePainterFallback, density, layoutDirection)
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Snapshot camera position so it can be restored when navigating back
            mapState.value?.cameraPosition?.let { pos ->
                pos.target?.let { target ->
                    onCameraIdleRef.value(target.latitude, target.longitude, pos.zoom, pos.bearing, pos.tilt)
                }
            }
            onMapDisposed()
        }
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

    // Register all catalog icon bitmaps for Google (gradient border) and OSM (yellow border) on style ready
    LaunchedEffect(styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        allIconKeys.forEach { key ->
            val painter = allPainters[key] ?: placePainterFallback
            style.addImage("pin-google-$key", createPoiCircle(key, PoiSource.GOOGLE, painter, density, layoutDirection))
            style.addImage("pin-osm-$key", createPoiCircle(key, PoiSource.OSM, painter, density, layoutDirection))
        }
    }

    // Re-render POIs whenever data or style readiness changes
    LaunchedEffect(pois, groups, styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val source = style.getSource("poi-source") as? GeoJsonSource ?: return@LaunchedEffect

        val hiddenGroupIds = groups.filter { !it.isVisible }.map { it.id }.toSet()
        val features = pois.filter { poi ->
            poi.isVisible && (poi.groupId == null || poi.groupId !in hiddenGroupIds)
        }.map { poi ->
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

    // Fly to requested location and reset bearing to north
    LaunchedEffect(flyToLocation) {
        val (lat, lng) = flyToLocation ?: return@LaunchedEffect
        val map = mapState.value ?: return@LaunchedEffect
        val camera = CameraPosition.Builder()
            .target(LatLng(lat, lng))
            .zoom(17.0)
            .bearing(0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera))
    }

    // Re-render saved route polylines whenever routes/points/visibility change
    LaunchedEffect(routes, routePoints, styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val source = style.getSource("saved-routes-source") as? GeoJsonSource ?: return@LaunchedEffect

        val features = routes.filter { it.isVisible }.mapNotNull { route ->
            val pts = routePoints[route.id] ?: return@mapNotNull null
            if (pts.size < 2) return@mapNotNull null
            val linePoints = pts.map { Point.fromLngLat(it.lng, it.lat) }
            // Strip leading #FF alpha prefix if present (stored as #AARRGGBB, MapLibre needs #RRGGBB)
            val mapColor = route.color.let { c ->
                if (c.length == 9 && c.startsWith("#")) "#${c.substring(3)}" else c
            }
            val props = JsonObject().apply {
                addProperty("routeId", route.id)
                addProperty("routeColor", mapColor)
            }
            Feature.fromGeometry(LineString.fromLngLats(linePoints), props)
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    // Update live route line color when the user changes it mid-recording
    LaunchedEffect(liveRouteColor, styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val mapColor = liveRouteColor.let { c ->
            if (c.length == 9 && c.startsWith("#")) "#${c.substring(3)}" else c
        }
        (style.getLayer("live-route-line") as? LineLayer)
            ?.setProperties(PropertyFactory.lineColor(mapColor))
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

    LaunchedEffect(googlePlaces, styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val source = style.getSource("google-places-source") as? GeoJsonSource ?: return@LaunchedEffect
        val features = googlePlaces.map { poi ->
            val iconId = "pin-google-${poi.iconKey ?: "place"}"
            Feature.fromGeometry(
                Point.fromLngLat(poi.lng, poi.lat),
                null,
                poi.id,
            ).apply {
                addStringProperty("poiId", poi.id)
                addStringProperty("icon-id", iconId)
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(osmPois, styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val source = style.getSource("osm-poi-source") as? GeoJsonSource ?: return@LaunchedEffect
        val features = osmPois.map { poi ->
            val iconId = "pin-osm-${poi.iconKey ?: "place"}"
            Feature.fromGeometry(
                Point.fromLngLat(poi.lng, poi.lat),
                null,
                poi.id,
            ).apply {
                addStringProperty("poiId", poi.id)
                addStringProperty("icon-id", iconId)
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(bulkPois, groups, styleReady.value) {
        val map = mapState.value ?: return@LaunchedEffect
        if (!styleReady.value) return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        val source = style.getSource("bulk-poi-source") as? GeoJsonSource ?: return@LaunchedEffect

        // Build a colour lookup for each group
        val groupColorMap = groups.associate { it.id to it.color }

        // Register a bitmap for each unique (iconKey, groupColor) combo not already registered
        val registered = mutableSetOf<String>()
        bulkPois.forEach { poi ->
            val resolvedIcon = poi.iconKey ?: "place"  // always use circle, never teardrop pin
            val groupColor = groupColorMap[poi.groupId] ?: "#2196F3"
            val bitmapKey = "pin-bulk-$resolvedIcon-$groupColor"
            if (registered.add(bitmapKey)) {
                val painter = allPainters[resolvedIcon] ?: placePainterFallback
                style.addImage(bitmapKey, createPoiCircle(resolvedIcon, PoiSource.BULK, painter, density, layoutDirection))
            }
        }

        val features = bulkPois.map { poi ->
            val resolvedIcon = poi.iconKey ?: "place"
            val groupColor = groupColorMap[poi.groupId] ?: "#2196F3"
            val iconId = "pin-bulk-$resolvedIcon-$groupColor"
            Feature.fromGeometry(
                Point.fromLngLat(poi.lng, poi.lat),
                null,
                poi.id,
            ).apply {
                addStringProperty("poiId", poi.id)
                addStringProperty("icon-id", iconId)
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    AndroidView(
        factory = {
            mapView.addOnDidFailLoadingMapListener {
                onMapErrorRef.value("Map failed to load. Check your API key or connection.")
            }
            mapView.getMapAsync { map ->
                mapState.value = map
                map.uiSettings.isCompassEnabled = false
                map.setStyle(Style.Builder().fromUri(styleUrl())) { style ->
                    style.addSource(
                        GeoJsonSource("poi-source", FeatureCollection.fromFeatures(emptyList<Feature>()))
                    )
                    style.addSource(GeoJsonSource("saved-routes-source", FeatureCollection.fromFeatures(emptyList<Feature>())))
                    style.addSource(GeoJsonSource("live-route-source"))
                    style.addLayer(
                        LineLayer("saved-routes-lines", "saved-routes-source").withProperties(
                            PropertyFactory.lineColor(Expression.get("routeColor")),
                            PropertyFactory.lineWidth(3f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        )
                    )
                    style.addLayer(
                        LineLayer("live-route-line", "live-route-source").withProperties(
                            PropertyFactory.lineColor("#FF5722"),
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        )
                    )
                    // OSM POI layer (below bulk and Google Places) — circle icons, centered
                    style.addSource(GeoJsonSource("osm-poi-source", FeatureCollection.fromFeatures(emptyList<Feature>())))
                    style.addLayer(
                        SymbolLayer("osm-poi-symbols", "osm-poi-source").withProperties(
                            PropertyFactory.iconImage(Expression.get("icon-id")),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                        )
                    )
                    // Bulk imported POIs layer (above OSM, below Google Places) — circle icons, centered
                    style.addSource(GeoJsonSource("bulk-poi-source", FeatureCollection.fromFeatures(emptyList<Feature>())))
                    style.addLayer(
                        SymbolLayer("bulk-poi-symbols", "bulk-poi-source").withProperties(
                            PropertyFactory.iconImage(Expression.get("icon-id")),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                        )
                    )
                    // Google Places layer (above bulk, below user POIs) — circle icons, centered
                    style.addSource(GeoJsonSource("google-places-source", FeatureCollection.fromFeatures(emptyList<Feature>())))
                    style.addLayer(
                        SymbolLayer("google-places-symbols", "google-places-source").withProperties(
                            PropertyFactory.iconImage(Expression.get("icon-id")),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
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
                        val rect = RectF(pt.x - 24f, pt.y - 24f, pt.x + 24f, pt.y + 24f)
                        // User POIs take highest priority
                        val poiHit = map.queryRenderedFeatures(rect, "poi-symbols")
                        if (poiHit.isNotEmpty()) {
                            val poiId = poiHit[0].getStringProperty("poiId")
                            if (poiId != null) {
                                onPoiTappedRef.value(poiId)
                                return@addOnMapClickListener true
                            }
                        }
                        // Google Places second
                        val googleHit = map.queryRenderedFeatures(rect, "google-places-symbols")
                        if (googleHit.isNotEmpty()) {
                            val placeId = googleHit[0].getStringProperty("poiId")
                            if (placeId != null) {
                                onGooglePlaceTappedRef.value(placeId)
                                return@addOnMapClickListener true
                            }
                        }
                        // Bulk imported POIs third
                        val bulkHit = map.queryRenderedFeatures(rect, "bulk-poi-symbols")
                        if (bulkHit.isNotEmpty()) {
                            val bulkId = bulkHit[0].getStringProperty("poiId")
                            if (bulkId != null) {
                                onBulkPoiTappedRef.value(bulkId)
                                return@addOnMapClickListener true
                            }
                        }
                        // OSM POIs fourth
                        val osmHit = map.queryRenderedFeatures(rect, "osm-poi-symbols")
                        if (osmHit.isNotEmpty()) {
                            val osmId = osmHit[0].getStringProperty("poiId")
                            if (osmId != null) {
                                onOsmPoiTappedRef.value(osmId)
                                return@addOnMapClickListener true
                            }
                        }
                        // Saved routes — wider tolerance for thin lines
                        val routeRect = RectF(pt.x - 30f, pt.y - 30f, pt.x + 30f, pt.y + 30f)
                        val routeHit = map.queryRenderedFeatures(routeRect, "saved-routes-lines")
                        if (routeHit.isNotEmpty()) {
                            val routeId = routeHit[0].getStringProperty("routeId")
                            if (routeId != null) {
                                onRouteTappedRef.value(routeId)
                                return@addOnMapClickListener true
                            }
                        }
                        false
                    }
                    // Save camera position whenever the user stops panning/zooming
                    map.addOnCameraIdleListener {
                        val pos = map.cameraPosition
                        val target = pos.target ?: return@addOnCameraIdleListener
                        onCameraIdleRef.value(
                            target.latitude,
                            target.longitude,
                            pos.zoom,
                            pos.bearing,
                            pos.tilt,
                        )
                        val bounds = map.projection.visibleRegion.latLngBounds
                        onBoundsChangedRef.value(
                            bounds.getLatNorth(),
                            bounds.getLatSouth(),
                            bounds.getLonEast(),
                            bounds.getLonWest(),
                            target.latitude,
                            target.longitude,
                            pos.zoom,
                            pos.bearing,
                            pos.tilt,
                        )
                    }
                    // Restore last known viewport (overrides default world view)
                    initialCamera?.let { cam ->
                        val restored = CameraPosition.Builder()
                            .target(org.maplibre.android.geometry.LatLng(cam.lat, cam.lng))
                            .zoom(cam.zoom)
                            .bearing(cam.bearing)
                            .tilt(cam.tilt)
                            .build()
                        map.moveCamera(CameraUpdateFactory.newCameraPosition(restored))
                    }
                    styleReady.value = true
                    onMapReady(map)
                }
            }
            mapView
        },
        modifier = modifier,
    )
}
