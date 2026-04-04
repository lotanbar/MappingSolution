package com.mappingsolution.data.recording.processing

import android.location.Location
import com.mappingsolution.data.recording.RecordingPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Orchestrates the full smart-track pipeline for each GPS fix:
 *
 *   Raw GPS fix
 *     → [GpsKalmanFilter]      — removes high-frequency noise; uses Location.accuracy as dynamic R
 *     → [RoadSnapper]          — queries MapLibre tile data (already in memory) for road geometry;
 *                                includes bearing filter and max-jump guard
 *     → [TrackModeManager]     — hysteresis: require 2 consecutive snapped fixes before trusting
 *                                road mode; fall back to Kalman if not yet stable
 *     → emit [RecordingPoint]
 *
 * Accuracy and min-movement pre-filters are applied upstream in RecordingService
 * before calling [process].
 */
@Singleton
class SmartTrackProcessor @Inject constructor() {

    private val kalmanFilter = GpsKalmanFilter()
    private val roadSnapper = RoadSnapper()
    private val modeManager = TrackModeManager()

    /** The last point emitted to the track (used for the max-jump guard). */
    private var lastEmitted: Pair<Double, Double>? = null

    /** Reset all stateful components. Call when a new recording is started. */
    fun reset() {
        kalmanFilter.reset()
        modeManager.reset()
        lastEmitted = null
    }

    /**
     * Process one GPS fix through the full pipeline and return the final [RecordingPoint].
     *
     * [map] may be null when the map is not yet ready; in that case the
     * Kalman-smoothed position is used directly (road snapping is skipped silently).
     *
     * Road snapping must run on the main thread (MapLibre rendering requirement).
     * The caller's coroutine context determines the thread; [withContext] switches
     * only if needed.
     */
    suspend fun process(location: Location, map: MapLibreMap?): RecordingPoint {
        val accuracy = if (location.hasAccuracy()) location.accuracy else MAX_ACCURACY_FALLBACK
        val (smoothLat, smoothLng) = kalmanFilter.process(
            lat = location.latitude,
            lng = location.longitude,
            accuracyMeters = accuracy,
            timestampMs = System.currentTimeMillis(),
        )

        // Travel bearing from the device (degrees, 0 = north). Available when moving.
        val travelBearing: Float? = if (location.hasBearing()) location.bearing else null

        // Max jump: how far from the last emitted point are we allowed to snap?
        // Use 3× the raw GPS movement this step, with a floor of SNAP_RADIUS_METERS.
        // This stops the snapper from teleporting to a motorway that's geometrically
        // close on-screen but physically elevated/separated.
        val prev = lastEmitted
        val maxJumpMeters: Double = if (prev != null) {
            val rawMove = haversineMeters(prev.first, prev.second, smoothLat, smoothLng)
            max(rawMove * MAX_JUMP_MULTIPLIER, MIN_JUMP_FLOOR_METERS)
        } else {
            RoadSnapper.SNAP_RADIUS_METERS
        }

        val snapped: Pair<Double, Double>? = if (map != null) {
            withContext(Dispatchers.Main) {
                // Wrap in runCatching so any MapLibre rendering-thread assertion or
                // null-style transient errors don't crash the recording session.
                runCatching {
                    roadSnapper.snap(
                        smoothLat = smoothLat,
                        smoothLng = smoothLng,
                        map = map,
                        travelBearingDeg = travelBearing,
                        previousLat = prev?.first,
                        previousLng = prev?.second,
                        maxAllowedJumpMeters = maxJumpMeters,
                    )
                }.getOrNull()
            }
        } else null

        // Update hysteresis. Mode switches to ROAD only after ROAD_ENTER_COUNT consecutive
        // snapped fixes, preventing a single rogue snap from jumping the track.
        val mode = modeManager.onSnappedResult(snapped != null)

        // Only use the snapped position when the mode manager has confirmed we are
        // stably on a road (ROAD mode). Before that threshold is reached, the snapper
        // may have found a road but we're not yet confident enough to commit to it.
        val (finalLat, finalLng) = if (mode == TrackMode.ROAD && snapped != null) {
            snapped
        } else {
            smoothLat to smoothLng
        }

        lastEmitted = finalLat to finalLng
        return RecordingPoint(ts = System.currentTimeMillis(), lat = finalLat, lng = finalLng)
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val sinDLat = Math.sin(dLat / 2)
        val sinDLon = Math.sin(dLon / 2)
        val a = sinDLat * sinDLat +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sinDLon * sinDLon
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private companion object {
        const val MAX_ACCURACY_FALLBACK = 20f

        /** The snap destination must be within this multiplier × actual GPS movement. */
        const val MAX_JUMP_MULTIPLIER = 3.0

        /** Minimum floor for the jump guard — don't make it tighter than this. */
        const val MIN_JUMP_FLOOR_METERS = 15.0
    }
}
