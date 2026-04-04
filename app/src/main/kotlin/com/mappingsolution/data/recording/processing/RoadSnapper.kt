package com.mappingsolution.data.recording.processing

import android.graphics.PointF
import android.graphics.RectF
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.Point
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Snaps a GPS position to the nearest road geometry visible in MapLibre.
 *
 * Works entirely offline — road geometries come from the tiles that are
 * already rendered in memory because the map auto-follows the user.
 *
 * Must be called on the main (rendering) thread.
 */
class RoadSnapper {

    companion object {
        /** Maximum distance from smoothed position to accept a road snap (metres). */
        const val SNAP_RADIUS_METERS = 25.0

        /**
         * Additional virtual distance penalty added to road segments that run
         * roughly perpendicular to the user's direction of travel (degrees → metres).
         * A road at 90° to travel is penalised by this amount; roads within 45° are unaffected.
         */
        private const val BEARING_WEIGHT_METERS = 6.0
    }

    /**
     * Tries to project [smoothLat]/[smoothLng] onto the nearest road segment
     * that is currently rendered by [map].
     *
     * Queries every layer whose ID starts with "road" — the set present in
     * the MapTiler satellite-hybrid style (e.g. road_motorway, road_primary,
     * road_secondary, road_tertiary, road_minor, road_path …).
     *
     * @param travelBearingDeg  GPS heading in degrees (0 = north). When provided, roads
     *                          running perpendicular to travel are penalised in the scoring.
     * @param previousLat       Last emitted track point latitude. When provided together with
     *                          [previousLng] and [maxAllowedJumpMeters], a snap that would
     *                          displace the track more than [maxAllowedJumpMeters] from the
     *                          previous point is rejected to prevent sudden teleports.
     * @param previousLng       Last emitted track point longitude.
     * @param maxAllowedJumpMeters  Maximum acceptable displacement from [previousLat]/[previousLng].
     *
     * @return  snapped (lat, lng) if a suitable road is within [SNAP_RADIUS_METERS], or null.
     */
    fun snap(
        smoothLat: Double,
        smoothLng: Double,
        map: MapLibreMap,
        travelBearingDeg: Float? = null,
        previousLat: Double? = null,
        previousLng: Double? = null,
        maxAllowedJumpMeters: Double = SNAP_RADIUS_METERS,
    ): Pair<Double, Double>? {
        val style = map.style ?: return null

        // Collect road layer IDs from the currently loaded style dynamically
        val roadLayerIds = style.layers
            .filter { it.id.startsWith("road") }
            .map { it.id }
            .toTypedArray()

        if (roadLayerIds.isEmpty()) return null

        // Convert the smoothed GPS position to screen pixel coordinates
        val center: PointF = map.projection.toScreenLocation(LatLng(smoothLat, smoothLng))

        // Derive a pixel radius that corresponds to SNAP_RADIUS_METERS at the
        // current zoom/projection by offsetting one radius-length north and
        // measuring the resulting screen-space distance.
        val offsetLat = smoothLat + SNAP_RADIUS_METERS / 111_111.0
        val offsetScreen: PointF = map.projection.toScreenLocation(LatLng(offsetLat, smoothLng))
        val pixelRadius = abs(offsetScreen.y - center.y).coerceAtLeast(10f)

        val queryRect = RectF(
            center.x - pixelRadius,
            center.y - pixelRadius,
            center.x + pixelRadius,
            center.y + pixelRadius,
        )

        val features = map.queryRenderedFeatures(queryRect, *roadLayerIds)
        if (features.isEmpty()) return null

        // Walk every segment of every returned road feature.
        // Each segment is scored as: geometric distance + bearing penalty.
        // The bearing penalty discourages snapping to roads running perpendicular
        // to the user's direction of travel (e.g. a cross-street or motorway ramp).
        var bestScore = Double.MAX_VALUE
        var bestDistMeters = Double.MAX_VALUE
        var bestLat = smoothLat
        var bestLng = smoothLng

        for (feature in features) {
            val lineGroups: List<List<Point>> = when (val geom = feature.geometry()) {
                is LineString -> listOf(geom.coordinates())
                is MultiLineString -> geom.coordinates()
                else -> continue
            }
            for (line in lineGroups) {
                for (i in 0 until line.size - 1) {
                    val a = line[i]
                    val b = line[i + 1]
                    val (cLat, cLng) = closestPointOnSegment(
                        smoothLat, smoothLng,
                        a.latitude(), a.longitude(),
                        b.latitude(), b.longitude(),
                    )
                    val dist = haversineMeters(smoothLat, smoothLng, cLat, cLng)

                    // Bearing penalty: 0 for roads within 45° of travel direction;
                    // scales up to BEARING_WEIGHT_METERS for roads at 90° (perpendicular).
                    val bearingPenalty = if (travelBearingDeg != null) {
                        val segBear = segmentBearing(
                            a.latitude(), a.longitude(), b.latitude(), b.longitude()
                        )
                        val perp = bearingPerpendicularity(travelBearingDeg.toDouble(), segBear)
                        if (perp > 45.0) ((perp - 45.0) / 45.0) * BEARING_WEIGHT_METERS else 0.0
                    } else 0.0

                    val score = dist + bearingPenalty
                    if (score < bestScore) {
                        bestScore = score
                        bestDistMeters = dist
                        bestLat = cLat
                        bestLng = cLng
                    }
                }
            }
        }

        if (bestDistMeters > SNAP_RADIUS_METERS) return null

        // Max-jump guard: reject snaps that would teleport the track far beyond
        // where the user actually moved this step (e.g. snapping to an overhead
        // motorway whose 2-D geometry overlaps a street below).
        if (previousLat != null && previousLng != null) {
            val jumpDist = haversineMeters(previousLat, previousLng, bestLat, bestLng)
            if (jumpDist > maxAllowedJumpMeters) return null
        }

        return bestLat to bestLng
    }

    /** Projects point P onto segment AB, clamping to the segment endpoints. */
    private fun closestPointOnSegment(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double,
    ): Pair<Double, Double> {
        val abLat = bLat - aLat
        val abLng = bLng - aLng
        val dot = abLat * abLat + abLng * abLng
        if (dot == 0.0) return aLat to aLng  // degenerate zero-length segment
        val t = ((pLat - aLat) * abLat + (pLng - aLng) * abLng).div(dot).coerceIn(0.0, 1.0)
        return (aLat + t * abLat) to (aLng + t * abLng)
    }

    /**
     * Compass bearing of segment A→B in degrees [0, 360).
     */
    private fun segmentBearing(
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double,
    ): Double {
        val dLon = Math.toRadians(bLng - aLng)
        val x = sin(dLon) * cos(Math.toRadians(bLat))
        val y = cos(Math.toRadians(aLat)) * sin(Math.toRadians(bLat)) -
                sin(Math.toRadians(aLat)) * cos(Math.toRadians(bLat)) * cos(dLon)
        return (Math.toDegrees(atan2(x, y)) + 360) % 360
    }

    /**
     * Returns the perpendicularity [0, 90] between a travel bearing and a road segment bearing.
     * 0 = road runs parallel to travel (best); 90 = road runs perpendicular (worst).
     * Roads are bidirectional, so a 180° flip is treated as 0° difference.
     */
    private fun bearingPerpendicularity(travelBear: Double, segBear: Double): Double {
        var diff = abs(travelBear - segBear) % 360.0
        if (diff > 180.0) diff = 360.0 - diff   // normalise to [0, 180]
        return min(diff, 180.0 - diff)            // bidirectional: [0, 90]
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
