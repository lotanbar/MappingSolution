package com.mappingsolution.data.recording

data class RecordingPoint(val ts: Long, val lat: Double, val lng: Double)

sealed class RecordingState {
    object Idle : RecordingState()

    data class Active(
        val routeId: String,
        val autoName: String,
        val startedAtMs: Long,
        val totalPausedMs: Long = 0L,
        val pausedSinceMs: Long? = null,
        val points: List<RecordingPoint> = emptyList(),
        val distanceMeters: Double = 0.0,
    ) : RecordingState() {
        val isPaused: Boolean get() = pausedSinceMs != null

        fun elapsedMs(nowMs: Long): Long {
            val extra = if (pausedSinceMs != null) nowMs - pausedSinceMs else 0L
            return (nowMs - startedAtMs - totalPausedMs - extra).coerceAtLeast(0L)
        }
    }
}
