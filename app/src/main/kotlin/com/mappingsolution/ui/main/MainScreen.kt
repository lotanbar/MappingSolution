package com.mappingsolution.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.mappingsolution.ui.main.components.BottomActionPanel
import com.mappingsolution.ui.main.components.MapComponent
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@Composable
fun MainScreen(
    onOpenLibrary: () -> Unit,
    onAddPoi: (lat: Double, lng: Double) -> Unit,
    onPoiTapped: (poiId: Long) -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pois by viewModel.pois.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var isFetchingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                isFetchingLocation = true
                val loc = withTimeoutOrNull(15_000L) { fetchCurrentLocation(context) }
                isFetchingLocation = false
                if (loc != null) {
                    onAddPoi(loc.first, loc.second)
                } else {
                    locationError = "Could not determine location. Try again outdoors or wait for a GPS fix."
                }
            }
        } else {
            locationError = "Location permission is required to add a POI at your current position."
        }
    }

    if (locationError != null) {
        AlertDialog(
            onDismissRequest = { locationError = null },
            title = { Text("Location Unavailable") },
            text = { Text(locationError!!) },
            confirmButton = {
                TextButton(onClick = { locationError = null }) { Text("OK") }
            },
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val mapHeight = maxHeight * 0.85f
        val panelHeight = maxHeight * 0.15f

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
                MapComponent(
                    pois = pois,
                    groups = groups,
                    onPoiTapped = onPoiTapped,
                    onMapError = { mapError = it },
                    modifier = Modifier.fillMaxSize(),
                )
                if (mapError != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFFB00020),
                            ),
                        ) {
                            Text(
                                text = mapError!!,
                                color = androidx.compose.ui.graphics.Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
            BottomActionPanel(
                onAddPoi = {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        scope.launch {
                            isFetchingLocation = true
                            val loc = withTimeoutOrNull(15_000L) { fetchCurrentLocation(context) }
                            isFetchingLocation = false
                            if (loc != null) {
                                onAddPoi(loc.first, loc.second)
                            } else {
                                locationError = "Could not determine location. Try again outdoors or wait for a GPS fix."
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                onRecordRoute = { /* commit 4 */ },
                onOpenLibrary = onOpenLibrary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight)
                    .background(MaterialTheme.colorScheme.surface),
            )
        }

        if (isFetchingLocation) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Card {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Getting location…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun fetchCurrentLocation(context: Context): Pair<Double, Double>? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
    // Try last-known location first (fast path)
    for (provider in providers) {
        runCatching {
            if (lm.isProviderEnabled(provider)) {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null && isValidCoordinate(loc.latitude, loc.longitude)) {
                    return loc.latitude to loc.longitude
                }
            }
        }
    }
    // Request a fresh fix from the best available enabled provider
    val bestProvider = providers.firstOrNull {
        runCatching { lm.isProviderEnabled(it) }.getOrDefault(false)
    } ?: return null

    return suspendCancellableCoroutine { cont ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lm.removeUpdates(this)
                if (!cont.isCompleted) {
                    val result = if (isValidCoordinate(location.latitude, location.longitude))
                        location.latitude to location.longitude else null
                    cont.resume(result)
                }
            }
            override fun onProviderDisabled(provider: String) {
                if (!cont.isCompleted) cont.resume(null)
            }
        }
        runCatching {
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(bestProvider, listener, Looper.getMainLooper())
            cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
        }.onFailure { if (!cont.isCompleted) cont.resume(null) }
    }
}

private fun isValidCoordinate(lat: Double, lng: Double) =
    lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)
