package com.mappingsolution

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mappingsolution.data.migration.LegacyDbMigration
import com.mappingsolution.data.util.StorageManager
import com.mappingsolution.service.RecordingService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

@HiltAndroidApp
class MappingApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val storageManager = StorageManager()
        val marker = File(storageManager.rootDir, ".migrated")
        if (!marker.exists()) {
            runBlocking(Dispatchers.IO) {
                LegacyDbMigration(this@MappingApplication, storageManager).run()
                marker.createNewFile()
            }
        }

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
