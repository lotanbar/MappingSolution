package com.mappingsolution.data.recording.processing

/**
 * Pure-Kotlin 2D Kalman filter for GPS track smoothing.
 *
 * Two independent 1-D filters (one per axis) each track
 * state = [position, velocity]. Measurement noise R is set
 * dynamically from Location.accuracy so a shakier fix is trusted less.
 */
class GpsKalmanFilter {

    // Tuning: process noise variances (degrees² and (deg/s)²)
    private val PROCESS_NOISE_POS = 1e-8
    private val PROCESS_NOISE_VEL = 1e-6

    // Per-axis state: x[0] = position (deg), x[1] = velocity (deg/s)
    private val latX = doubleArrayOf(0.0, 0.0)
    private val latP = Array(2) { r -> DoubleArray(2) { c -> if (r == c) 1.0 else 0.0 } }

    private val lngX = doubleArrayOf(0.0, 0.0)
    private val lngP = Array(2) { r -> DoubleArray(2) { c -> if (r == c) 1.0 else 0.0 } }

    private var lastTimestampMs = 0L
    private var initialized = false

    fun reset() {
        initialized = false
        lastTimestampMs = 0L
    }

    /**
     * Feed a raw GPS fix and get back the Kalman-smoothed position.
     *
     * @param accuracyMeters  Location.accuracy (1-sigma, metres)
     * @param timestampMs     System.currentTimeMillis() at fix time
     */
    fun process(
        lat: Double,
        lng: Double,
        accuracyMeters: Float,
        timestampMs: Long,
    ): Pair<Double, Double> {
        if (!initialized) {
            latX[0] = lat; latX[1] = 0.0
            lngX[0] = lng; lngX[1] = 0.0
            latP[0][0] = 1.0; latP[0][1] = 0.0; latP[1][0] = 0.0; latP[1][1] = 1.0
            lngP[0][0] = 1.0; lngP[0][1] = 0.0; lngP[1][0] = 0.0; lngP[1][1] = 1.0
            lastTimestampMs = timestampMs
            initialized = true
            return lat to lng
        }

        val dt = ((timestampMs - lastTimestampMs) / 1000.0).coerceIn(0.5, 30.0)
        lastTimestampMs = timestampMs

        // Convert accuracy from metres to degrees, then square for variance
        val accuracyDeg = accuracyMeters / 111_111.0
        val r = accuracyDeg * accuracyDeg

        val smoothLat = updateAxis(latX, latP, lat, dt, r)
        val smoothLng = updateAxis(lngX, lngP, lng, dt, r)

        return smoothLat to smoothLng
    }

    /**
     * One step of the 1-D Kalman filter with state [pos, vel].
     * Mutates [x] and [p] in place, returns the updated position.
     *
     * State-transition F = [[1, dt], [0, 1]]
     * Measurement matrix H = [1, 0]
     */
    private fun updateAxis(
        x: DoubleArray,
        p: Array<DoubleArray>,
        z: Double,
        dt: Double,
        r: Double,
    ): Double {
        // Predict
        val predPos = x[0] + x[1] * dt
        val predVel = x[1]

        val pp00 = p[0][0] + dt * (p[1][0] + p[0][1]) + dt * dt * p[1][1] + PROCESS_NOISE_POS
        val pp01 = p[0][1] + dt * p[1][1]
        val pp10 = p[1][0] + dt * p[1][1]
        val pp11 = p[1][1] + PROCESS_NOISE_VEL

        // Update
        val s = pp00 + r           // innovation covariance
        val k0 = pp00 / s          // Kalman gain for position
        val k1 = pp10 / s          // Kalman gain for velocity
        val innov = z - predPos

        x[0] = predPos + k0 * innov
        x[1] = predVel + k1 * innov

        p[0][0] = (1.0 - k0) * pp00
        p[0][1] = (1.0 - k0) * pp01
        p[1][0] = pp10 - k1 * pp00
        p[1][1] = pp11 - k1 * pp01

        return x[0]
    }
}
