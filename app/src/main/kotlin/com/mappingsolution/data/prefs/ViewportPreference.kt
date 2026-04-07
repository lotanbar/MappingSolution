package com.mappingsolution.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewportPreference @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("viewport", Context.MODE_PRIVATE)

    fun save(lat: Double, lng: Double, zoom: Double, bearing: Double = 0.0, tilt: Double = 0.0) {
        prefs.edit()
            .putLong("lat", lat.toBits())
            .putLong("lng", lng.toBits())
            .putLong("zoom", zoom.toBits())
            .putLong("bearing", bearing.toBits())
            .putLong("tilt", tilt.toBits())
            .apply()
    }

    fun load(): SavedCamera? {
        if (!prefs.contains("lat")) return null
        return SavedCamera(
            lat = Double.fromBits(prefs.getLong("lat", 0L)),
            lng = Double.fromBits(prefs.getLong("lng", 0L)),
            zoom = Double.fromBits(prefs.getLong("zoom", java.lang.Double.doubleToLongBits(12.0))),
            bearing = Double.fromBits(prefs.getLong("bearing", 0L)),
            tilt = Double.fromBits(prefs.getLong("tilt", 0L)),
        )
    }

    data class SavedCamera(
        val lat: Double,
        val lng: Double,
        val zoom: Double,
        val bearing: Double,
        val tilt: Double,
    )
}
