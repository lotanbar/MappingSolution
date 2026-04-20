package com.mappingsolution.data.prefs

import android.content.Context
import com.mappingsolution.data.map.MapStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapStylePreference @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("map_style", Context.MODE_PRIVATE)

    fun save(style: MapStyle) {
        prefs.edit().putString("style", style.name).apply()
    }

    fun load(): MapStyle =
        prefs.getString("style", null)
            ?.let { runCatching { MapStyle.valueOf(it) }.getOrNull() }
            ?: MapStyle.SATELLITE
}
