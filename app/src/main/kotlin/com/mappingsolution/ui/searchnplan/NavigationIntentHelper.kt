package com.mappingsolution.ui.searchnplan

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mappingsolution.data.model.PlanDestination

object NavigationIntentHelper {

    /**
     * Launches turn-by-turn navigation to a single destination.
     * Opens Waze if installed; falls back to Google Maps.
     */
    fun launchSingleNavigation(context: Context, lat: Double, lng: Double) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("waze://?ll=$lat,$lng&navigate=yes"))
            )
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/maps/dir/?api=1&destination=$lat,$lng"),
                )
            )
        }
    }

    /**
     * Launches navigation for one or more destinations.
     * Single stop: Waze first, Google Maps fallback.
     * Multiple stops: always Google Maps with all waypoints in order.
     */
    fun launchNavigation(context: Context, destinations: List<PlanDestination>) {
        when {
            destinations.isEmpty() -> return
            destinations.size == 1 -> launchSingleNavigation(
                context, destinations[0].lat, destinations[0].lng,
            )
            else -> {
                val stops = destinations.joinToString("/") { "${it.lat},${it.lng}" }
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://maps.google.com/maps/dir/$stops"),
                    )
                )
            }
        }
    }
}
