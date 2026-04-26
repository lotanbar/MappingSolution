package com.mappingsolution.ui.searchnplan

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

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
}
