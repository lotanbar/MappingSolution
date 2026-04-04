package com.mappingsolution

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mappingsolution.service.RecordingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MappingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RecordingService.NOTIF_CHANNEL_ID,
                "Route Recording",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shows active route recording status" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
