package com.mappingsolution.data.recording

import com.mappingsolution.data.fs.RouteFileRepository
import com.mappingsolution.data.model.Route
import com.mappingsolution.data.model.RoutePoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_ROUTE_COLOR = "#FFFF5722"

@Singleton
class RecordingRepository @Inject constructor(
    private val routeFileRepository: RouteFileRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RecordingEvent>(replay = 1, extraBufferCapacity = 0)
    val events: SharedFlow<RecordingEvent> = _events.asSharedFlow()

    fun consumeStoppedEvent() { _events.resetReplayCache() }

    fun updateState(state: RecordingState) { _state.value = state }

    suspend fun createRoute(): Pair<String, String> {
        val name = SimpleDateFormat("dd/MM/yyyy-HH:mm", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()
        val id = routeFileRepository.insert(
            Route(
                name = name,
                color = DEFAULT_ROUTE_COLOR,
                didUserTapStop = false,
                startedAt = now,
                checkpointAt = now,
            )
        )
        return id to name
    }

    suspend fun persistPointsSync(routeId: String, points: List<RecordingPoint>) {
        if (points.isEmpty()) return
        routeFileRepository.appendPoints(
            routeId,
            points.map { RoutePoint(ts = it.ts, lat = it.lat, lng = it.lng) },
        )
    }

    fun persistPoints(routeId: String, points: List<RecordingPoint>, orderOffset: Int) {
        if (points.isEmpty()) return
        scope.launch {
            routeFileRepository.appendPoints(
                routeId,
                points.map { RoutePoint(ts = it.ts, lat = it.lat, lng = it.lng) }
            )
        }
    }

    suspend fun finalizeStop(routeId: String, distanceMeters: Double, durationSec: Long) {
        val existing = routeFileRepository.getById(routeId) ?: return
        val dateStr = SimpleDateFormat("dd/MM/yyyy-HH:mm", Locale.getDefault()).format(Date(existing.startedAt))
        val fullName = "$dateStr-${formatDuration(durationSec)}-${formatDistance(distanceMeters)}"
        routeFileRepository.update(
            existing.copy(
                name = fullName,
                didUserTapStop = true,
                stoppedAt = System.currentTimeMillis(),
                distanceMeters = distanceMeters,
                durationSec = durationSec,
            )
        )
        _state.value = RecordingState.Idle
        _events.emit(RecordingEvent.Stopped(routeId))
    }

    suspend fun getIncompleteRoutes() = routeFileRepository.getIncomplete()

    private fun formatDuration(sec: Long): String {
        val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000) "%.2fkm".format(meters / 1000) else "%.0fm".format(meters)
}
