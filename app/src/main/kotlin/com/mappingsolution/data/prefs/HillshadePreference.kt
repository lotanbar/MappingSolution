package com.mappingsolution.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HillshadePreference @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("map_layers", Context.MODE_PRIVATE)

    fun save(visible: Boolean) {
        prefs.edit().putBoolean("hillshade_visible", visible).apply()
    }

    fun load(): Boolean = prefs.getBoolean("hillshade_visible", true)
}
