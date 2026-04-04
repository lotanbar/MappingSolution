package com.mappingsolution.data.recording

import com.mappingsolution.data.db.entity.RouteEntity
import com.mappingsolution.data.db.entity.RoutePointEntity
import com.mappingsolution.data.repository.RouteRepository
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
    private val routeRepository: RouteRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RecordingEvent>(replay = 1, extraBufferCapacity = 0)
    val events: SharedFlow<RecordingEvent> = _events.asSharedFlow()

    /** Call after consuming a [RecordingEvent.Stopped] to clear the replay cache. */
    fun consumeStoppedEvent() { _events.resetReplayCache() }

    fun updateState(state: RecordingState) {
        _state.value = state
    }

    /** Creates a new incomplete route row and returns its id and auto-generated name. */
    suspend fun createRoute(): Pair<Long, String> {
        val name = SimpleDateFormat("dd/MM/yyyy-HH:mm", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()
        val id = routeRepository.insert(
            RouteEntity(
                name = name,
                color = DEFAULT_ROUTE_COLOR,
                didUserTapStop = false,
                startedAt = now,
                checkpointAt = now,
            )
        )
        return id to name
    }

    suspend fun persistPointsSync(routeId: Long, points: List<RecordingPoint>) {
        if (points.isEmpty()) return
        val entities = points.mapIndexed { i, pt ->
            RoutePointEntity(
                routeId = routeId,
                ts = pt.ts,
                lat = pt.lat,
                lng = pt.lng,
                orderIndex = i,
            )
        }
        routeRepository.appendPoints(entities)
    }

    fun persistPoints(routeId: Long, points: List<RecordingPoint>, orderOffset: Int) {
        if (points.isEmpty()) return
        scope.launch {
            val entities = points.mapIndexed { i, pt ->
                RoutePointEntity(
                    routeId = routeId,
                    ts = pt.ts,
                    lat = pt.lat,
                    lng = pt.lng,
                    orderIndex = orderOffset + i,
                )
            }
            routeRepository.appendPoints(entities)
        }
    }

    suspend fun finalizeStop(routeId: Long, distanceMeters: Double, durationSec: Long) {
        val existing = routeRepository.getById(routeId) ?: return
        val dateStr = SimpleDateFormat("dd/MM/yyyy-HH:mm", Locale.getDefault()).format(Date(existing.startedAt))
        val fullName = "$dateStr-${formatDuration(durationSec)}-${formatDistance(distanceMeters)}"
        routeRepository.update(
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

    private fun formatDuration(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000) "%.2fkm".format(meters / 1000) else "%.0fm".format(meters)
}
