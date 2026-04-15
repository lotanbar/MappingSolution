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
 *     → post-Kalman jump guard — discards fixes that land >120m from the previous processed position
 *     → [RoadSnapper]          — queries MapLibre tile data (already in memory) for road/path geometry
 *     → [TrackModeManager]     — hysteresis: require 2 consecutive snapped fixes before committing to road mode
 *     → spike detector         — buffers 1 fix of delay; if the previous fix jumped out and back
 *                                (ping-pong), it is replaced with a hold at the last stable position
 *     → emit [RecordingPoint]  — null means the fix was buffered/discarded; call [flushPending] on stop
 *
 * Accuracy and min-movement pre-filters are applied upstream in [RecordingService].
 */
@Singleton
class SmartTrackProcessor @Inject constructor() {

    private val kalmanFilter = GpsKalmanFilter()
    private val roadSnapper = RoadSnapper()
    private val modeManager = TrackModeManager()

    /**
     * The last position returned to the caller (used by the spike detector to detect returns).
     * Updated only when we actually emit a non-held point.
     */
    private var lastEmittedLatLng: Pair<Double, Double>? = null

    /**
     * The position computed on the PREVIOUS call, buffered for spike-detection.
     * We delay emission by one fix so the spike detector can compare prev→B→current.
     */
    private var pendingLatLng: Pair<Double, Double>? = null
    private var pendingTs: Long = 0L

    /** Last road-snap position accepted by the continuity guard. Null when off-road. */
    private var lastSnappedLatLng: Pair<Double, Double>? = null

    /** Reset all stateful components. Call when a new recording is started. */
    fun reset() {
        kalmanFilter.reset()
        modeManager.reset()
        lastEmittedLatLng = null
        pendingLatLng = null
        pendingTs = 0L
        lastSnappedLatLng = null
    }

    /**
     * Flush the last buffered (pending) point when recording stops.
     * Must be called from the same coroutine context as [process].
     */
    fun flushPending(): RecordingPoint? {
        val pending = pendingLatLng ?: return null
        val ts = pendingTs
        pendingLatLng = null
        lastEmittedLatLng = pending
        return RecordingPoint(ts = ts, lat = pending.first, lng = pending.second)
    }

    /**
     * Process one GPS fix through the full pipeline.
     *
     * Returns a [RecordingPoint] to emit, or **null** if this fix was buffered or discarded.
     * Null means: do nothing this cycle; the position will appear on the next call (or at stop
     * via [flushPending]).
     *
     * Road snapping must run on the main thread (MapLibre rendering requirement).
     */
    suspend fun process(location: Location, map: MapLibreMap?): RecordingPoint? {
        val accuracy = if (location.hasAccuracy()) location.accuracy else MAX_ACCURACY_FALLBACK
        val nowMs = System.currentTimeMillis()

        val (smoothLat, smoothLng) = kalmanFilter.process(
            lat = location.latitude,
            lng = location.longitude,
            accuracyMeters = accuracy,
            timestampMs = nowMs,
        )

        // Post-Kalman jump guard: compare smoothed position against the PREVIOUS processed
        // position (pendingLatLng, one fix ago) — not lastEmittedLatLng which is two fixes ago.
        // If the Kalman state was poisoned by a bad upstream fix the smoothed output will still
        // be close to the raw bad position; reset the filter and discard this fix.
        val prevProcessed = pendingLatLng
        if (prevProcessed != null) {
            val smoothDist = haversineMeters(prevProcessed.first, prevProcessed.second, smoothLat, smoothLng)
            if (smoothDist > MAX_POST_KALMAN_JUMP_METERS) {
                kalmanFilter.reset()
                return null
            }
        }

        // Travel bearing from the device (degrees, 0 = north). Available when moving.
        val travelBearing: Float? = if (location.hasBearing()) location.bearing else null

        val maxJumpMeters: Double = if (prevProcessed != null) {
            val rawMove = haversineMeters(prevProcessed.first, prevProcessed.second, smoothLat, smoothLng)
            max(rawMove * MAX_JUMP_MULTIPLIER, MIN_JUMP_FLOOR_METERS)
        } else {
            RoadSnapper.SNAP_RADIUS_METERS
        }

        val rawSnapped: Pair<Double, Double>? = if (map != null) {
            withContext(Dispatchers.Main) {
                // Wrap in runCatching so any MapLibre rendering-thread assertion or
                // null-style transient errors don't crash the recording session.
                runCatching {
                    roadSnapper.snap(
                        smoothLat = smoothLat,
                        smoothLng = smoothLng,
                        map = map,
                        travelBearingDeg = travelBearing,
                        previousLat = prevProcessed?.first,
                        previousLng = prevProcessed?.second,
                        maxAllowedJumpMeters = maxJumpMeters,
                    )
                }.getOrNull()
            }
        } else null

        // Snap continuity guard: reject snaps that jump much further than the GPS actually
        // moved since the last accepted snap. Prevents oscillation between parallel road
        // features when the smoothed position sits equidistant between two roads.
        val snapped: Pair<Double, Double>? = if (rawSnapped != null) {
            val prevSnap = lastSnappedLatLng
            val prevProc = prevProcessed
            if (prevSnap != null && prevProc != null) {
                val snapJump = haversineMeters(prevSnap.first, prevSnap.second, rawSnapped.first, rawSnapped.second)
                val gpsMove = haversineMeters(prevProc.first, prevProc.second, smoothLat, smoothLng)
                if (snapJump > maxOf(gpsMove * SNAP_CONTINUITY_MULTIPLIER, SNAP_CONTINUITY_FLOOR_METERS)) null
                else rawSnapped
            } else rawSnapped
        } else null

        // Update hysteresis. Mode switches to ROAD only after ROAD_ENTER_COUNT consecutive
        // snapped fixes, preventing a single rogue snap from jumping the track.
        val mode = modeManager.onSnappedResult(snapped != null)

        // Track the last accepted snap; clear when off-road so a stale reference
        // doesn't constrain the first snap of the next road entry.
        if (snapped != null) lastSnappedLatLng = snapped
        else if (mode == TrackMode.OFF_ROAD) lastSnappedLatLng = null

        val (finalLat, finalLng) = if (mode == TrackMode.ROAD && snapped != null) {
            snapped
        } else {
            smoothLat to smoothLng
        }

        val currentLatLng = finalLat to finalLng

        // ── Spike detector (1-fix delay) ──────────────────────────────────────────────────────
        // We buffer the current position and emit the PREVIOUS one. Before emitting, we check
        // if the previous position (B) is a ping-pong spike: it jumped far from the last stable
        // emission (A) and the current position (C) came back near A.
        //
        //   Spike pattern: A──────B          A and C are close; B is a detour.
        //                   ╲____╱ C              → hold at A, discard B.
        //
        // This catches moderate GPS bounces (20–100 m) that slip under the speed threshold.
        val toEmit: RecordingPoint?
        if (prevProcessed == null) {
            // First fix ever — buffer it; nothing to emit yet.
            toEmit = null
        } else {
            val prevEmitted = lastEmittedLatLng
            val isSpike = prevEmitted != null && run {
                val dAB = haversineMeters(prevEmitted.first, prevEmitted.second, prevProcessed.first, prevProcessed.second)
                val dBC = haversineMeters(prevProcessed.first, prevProcessed.second, currentLatLng.first, currentLatLng.second)
                val dAC = haversineMeters(prevEmitted.first, prevEmitted.second, currentLatLng.first, currentLatLng.second)
                dAB > SPIKE_MIN_METERS && dBC > SPIKE_MIN_METERS &&
                        dAC < maxOf(dAB, dBC) * SPIKE_RETURN_RATIO
            }
            if (isSpike) {
                // Replace the spiked B with a hold at A (lastEmittedLatLng unchanged).
                val held = prevEmitted!!
                toEmit = RecordingPoint(ts = pendingTs, lat = held.first, lng = held.second)
            } else {
                lastEmittedLatLng = prevProcessed
                toEmit = RecordingPoint(ts = pendingTs, lat = prevProcessed.first, lng = prevProcessed.second)
            }
        }

        pendingLatLng = currentLatLng
        pendingTs = nowMs

        return toEmit
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

        /** Minimum floor for the road-snap jump guard. */
        const val MIN_JUMP_FLOOR_METERS = 15.0

        /**
         * Maximum plausible distance (metres) between consecutive processed positions after
         * Kalman smoothing. Sized for 150 km/h × ~2.7 s average GPS interval = ~112 m.
         * Fires when a bad fix poisons the Kalman state; the fix is discarded and Kalman reset.
         */
        const val MAX_POST_KALMAN_JUMP_METERS = 120.0

        /**
         * Spike detector: minimum one-step displacement for a point to be considered a candidate
         * spike. Jumps smaller than this are normal GPS noise and are ignored.
         */
        const val SPIKE_MIN_METERS = 12.0

        /**
         * Spike detector: a point B is considered a spike when the direct distance A→C is less
         * than this fraction of max(A→B, B→C). The lower the ratio, the stricter the detector
         * (requires C to be very close to A for a spike to be declared).
         *
         * 0.45 means: C must be within 45% of the departure distance from A.
         * Example: if A→B = 57 m, A→C must be < 26 m to flag as spike.
         */
        const val SPIKE_RETURN_RATIO = 0.45

        /**
         * Snap continuity guard: a new snap is rejected if it jumps more than
         * [SNAP_CONTINUITY_MULTIPLIER] × GPS movement from the last accepted snap position.
         * Prevents oscillation between parallel road features (e.g. road vs. sidewalk).
         */
        const val SNAP_CONTINUITY_MULTIPLIER = 2.0

        /** Minimum floor for the snap continuity jump guard. */
        const val SNAP_CONTINUITY_FLOOR_METERS = 15.0
    }
}
