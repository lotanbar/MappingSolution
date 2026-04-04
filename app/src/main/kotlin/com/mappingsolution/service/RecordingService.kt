package com.mappingsolution.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.mappingsolution.MainActivity
import com.mappingsolution.data.map.MapHolder
import com.mappingsolution.data.recording.RecordingEvent
import com.mappingsolution.data.recording.RecordingPoint
import com.mappingsolution.data.recording.RecordingRepository
import com.mappingsolution.data.recording.RecordingState
import com.mappingsolution.data.recording.processing.SmartTrackProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var recordingRepository: RecordingRepository
    @Inject lateinit var smartTrackProcessor: SmartTrackProcessor
    @Inject lateinit var mapHolder: MapHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null
    private val pendingPoints = mutableListOf<RecordingPoint>()
    private var flushedPointCount = 0
    private var lastLocation: Location? = null
    private var lastEmittedPoint: RecordingPoint? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onNewLocation(location)
        override fun onProviderDisabled(provider: String) = Unit
        override fun onProviderEnabled(provider: String) = Unit
    }

    companion object {
        const val ACTION_START = "com.mappingsolution.recording.START"
        const val ACTION_PAUSE = "com.mappingsolution.recording.PAUSE"
        const val ACTION_RESUME = "com.mappingsolution.recording.RESUME"
        const val ACTION_STOP = "com.mappingsolution.recording.STOP"
        const val NOTIF_CHANNEL_ID = "recording_channel"
        const val NOTIF_ID = 1001

        /** Only accept GPS fixes with an accuracy circle ≤ this value (meters). */
        private const val MAX_ACCURACY_METERS = 20f
        /** Minimum displacement before we count movement (filters stationary jitter). */
        private const val MIN_MOVEMENT_METERS = 3.0

        fun startIntent(context: Context) = Intent(context, RecordingService::class.java).apply { action = ACTION_START }
        fun pauseIntent(context: Context) = Intent(context, RecordingService::class.java).apply { action = ACTION_PAUSE }
        fun resumeIntent(context: Context) = Intent(context, RecordingService::class.java).apply { action = ACTION_RESUME }
        fun stopIntent(context: Context) = Intent(context, RecordingService::class.java).apply { action = ACTION_STOP }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIF_ID, buildNotification("Recording route…", "Starting…"))
                scope.launch(Dispatchers.IO) { handleStart() }
            }
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> scope.launch(Dispatchers.IO) { handleStop() }
            null -> {
                val current = recordingRepository.state.value
                if (current is RecordingState.Active) {
                    startForeground(NOTIF_ID, buildNotification("Recording route…", current.autoName))
                    startLocationUpdates()
                    startNotificationTicker()
                } else {
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private suspend fun handleStart() {
        val (routeId, name) = recordingRepository.createRoute()
        val now = System.currentTimeMillis()
        smartTrackProcessor.reset()
        lastLocation = null
        lastEmittedPoint = null
        recordingRepository.updateState(
            RecordingState.Active(
                routeId = routeId,
                autoName = name,
                startedAtMs = now,
            )
        )
        startLocationUpdates()
        startNotificationTicker()
    }

    private fun handlePause() {
        stopLocationUpdates()
        val current = recordingRepository.state.value as? RecordingState.Active ?: return
        flushPendingPoints(current.routeId)
        recordingRepository.updateState(current.copy(pausedSinceMs = System.currentTimeMillis()))
    }

    private fun handleResume() {
        val current = recordingRepository.state.value as? RecordingState.Active ?: return
        val now = System.currentTimeMillis()
        val pausedDuration = if (current.pausedSinceMs != null) now - current.pausedSinceMs else 0L
        recordingRepository.updateState(
            current.copy(
                totalPausedMs = current.totalPausedMs + pausedDuration,
                pausedSinceMs = null,
            )
        )
        startLocationUpdates()
    }

    private suspend fun handleStop() {
        stopLocationUpdates()
        val current = recordingRepository.state.value as? RecordingState.Active ?: run { stopSelf(); return }
        // Flush remaining points synchronously — finalizeStop renames the folder,
        // so we must finish writing before that rename happens.
        if (pendingPoints.isNotEmpty()) {
            val toFlush = pendingPoints.toList()
            pendingPoints.clear()
            recordingRepository.persistPointsSync(current.routeId, toFlush)
            flushedPointCount += toFlush.size
        }
        val now = System.currentTimeMillis()
        val durationSec = current.elapsedMs(now) / 1000L
        recordingRepository.finalizeStop(current.routeId, current.distanceMeters, durationSec)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            runCatching {
                if (lm.isProviderEnabled(provider)) {
                    lm.requestLocationUpdates(provider, 2000L, 2f, locationListener, Looper.getMainLooper())
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        runCatching { lm.removeUpdates(locationListener) }
    }

    private fun onNewLocation(location: Location) {
        val current = recordingRepository.state.value as? RecordingState.Active ?: return
        if (current.isPaused) return
        // Pre-filter: reject inaccurate fixes before entering the pipeline
        if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_METERS) return

        // Pre-filter: reject micro-jitter based on raw GPS displacement
        val last = lastLocation
        val rawDist = if (last != null)
            haversineMeters(last.latitude, last.longitude, location.latitude, location.longitude)
        else 0.0
        if (last != null && rawDist < MIN_MOVEMENT_METERS) return
        lastLocation = location

        scope.launch {
            // Run Kalman → road snap → mode hysteresis on the captured location
            val point = smartTrackProcessor.process(location, mapHolder.map)

            // Accumulate distance from the last emitted (smoothed/snapped) point
            val prevEmit = lastEmittedPoint
            val addedDistance = if (prevEmit != null)
                haversineMeters(prevEmit.lat, prevEmit.lng, point.lat, point.lng)
            else 0.0
            lastEmittedPoint = point

            // Re-read state; it may have changed while awaiting the road query
            val st = recordingRepository.state.value as? RecordingState.Active ?: return@launch
            if (st.isPaused) return@launch

            pendingPoints.add(point)
            val newPoints = st.points + point
            val newDistance = st.distanceMeters + addedDistance
            recordingRepository.updateState(st.copy(points = newPoints, distanceMeters = newDistance))

            if (pendingPoints.size >= 20) {
                flushPendingPoints(st.routeId)
            }
        }
    }

    private fun flushPendingPoints(routeId: String) {
        if (pendingPoints.isEmpty()) return
        val toFlush = pendingPoints.toList()
        pendingPoints.clear()
        recordingRepository.persistPoints(routeId, toFlush, flushedPointCount)
        flushedPointCount += toFlush.size
    }

    private fun startNotificationTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(1000L)
                val st = recordingRepository.state.value
                if (st !is RecordingState.Active) break
                val elapsed = formatElapsed(st.elapsedMs(System.currentTimeMillis()))
                val dist = formatDistance(st.distanceMeters)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, buildNotification("Recording: $elapsed", dist))
            }
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun formatElapsed(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000) "%.2f km".format(meters / 1000) else "%.0f m".format(meters)

    override fun onDestroy() {
        tickerJob?.cancel()
        super.onDestroy()
    }
}
