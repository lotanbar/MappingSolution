package com.mappingsolution.service

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mappingsolution.data.fs.RasterLayerRepository
import com.mappingsolution.data.map.MbTilesReader
import com.mappingsolution.data.model.RasterLayer
import com.mappingsolution.data.util.StorageManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@HiltWorker
class MbtilesImportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val storageManager: StorageManager,
    private val rasterLayerRepository: RasterLayerRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriString = inputData.getString(KEY_URI)
            ?: return@withContext Result.failure(workDataOf(KEY_ERROR to "No URI provided"))

        val uri = Uri.parse(uriString)
        var tempFile: File? = null

        return@withContext try {
            setForeground(buildForegroundInfo("Starting import…"))

            // 1. Copy URI → temp file with progress reporting
            tempFile = storageManager.getMbtilesTempFile()
            copyWithProgress(uri, tempFile)

            // 2. Read metadata from the temp file
            val metadata = MbTilesReader(tempFile.absolutePath).use { it.readMetadata() }
            val layerName = metadata["name"]?.trim()?.takeIf { it.isNotEmpty() }
                ?: uri.lastPathSegment?.removeSuffix(".mbtiles")?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Untitled Layer"
            val minZoom = metadata["minzoom"]?.toIntOrNull() ?: 0
            val maxZoom = metadata["maxzoom"]?.toIntOrNull() ?: 22

            // 3. Purge existing layer with the same name (idempotent re-import)
            val existing = rasterLayerRepository.findByName(layerName)
            if (existing != null) {
                rasterLayerRepository.delete(existing.id)
            }

            // 4. Move temp file to final path
            val newId = UUID.randomUUID().toString()
            val finalFile = storageManager.getMbtilesFile(layerName, newId)
            tempFile.renameTo(finalFile).also { success ->
                if (!success) {
                    // Fallback: copy + delete
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }
            }
            tempFile = null // ownership transferred

            // 5. Persist the new record
            val layer = RasterLayer(
                id = newId,
                name = layerName,
                filePath = finalFile.absolutePath,
                isVisible = true,
                minZoom = minZoom,
                maxZoom = maxZoom,
            )
            rasterLayerRepository.insert(layer)

            Result.success(workDataOf(KEY_LAYER_NAME to layerName))
        } catch (e: Exception) {
            android.util.Log.e("MbtilesImportWorker", "MBTiles import failed", e)
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unexpected error")))
        } finally {
            tempFile?.takeIf { it.exists() }?.delete()
        }
    }

    private suspend fun copyWithProgress(uri: Uri, dest: File) {
        val isFileUri = uri.scheme == "file"
        val totalBytes = if (isFileUri) {
            File(uri.path!!).length().takeIf { it > 0L } ?: -1L
        } else {
            context.contentResolver.openFileDescriptor(uri, "r")
                ?.use { it.statSize }
                ?.takeIf { it > 0L } ?: -1L
        }

        val inputStream = if (isFileUri) java.io.FileInputStream(uri.path!!)
                          else context.contentResolver.openInputStream(uri)
        inputStream?.use { input ->
            FileOutputStream(dest).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesCopied = 0L
                var lastReported = 0L

                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesCopied += read

                    // Report progress at most every ~4 MB to avoid flooding WorkManager
                    if (bytesCopied - lastReported >= PROGRESS_INTERVAL) {
                        lastReported = bytesCopied
                        val text = buildProgressText(bytesCopied, totalBytes)
                        setForeground(buildForegroundInfo(text))
                        setProgress(
                            workDataOf(
                                KEY_BYTES_COPIED to bytesCopied,
                                KEY_BYTES_TOTAL to totalBytes,
                            )
                        )
                    }
                }
            }
        } ?: error("Cannot open input stream for URI: $uri")
    }

    private fun buildProgressText(copied: Long, total: Long): String {
        val copiedMb = copied / 1_048_576.0
        return if (total > 0) {
            val totalMb = total / 1_048_576.0
            "Copying… %.1f MB / %.1f MB".format(copiedMb, totalMb)
        } else {
            "Copying… %.1f MB".format(copiedMb)
        }
    }

    private fun buildForegroundInfo(contentText: String): ForegroundInfo {
        val notification = buildNotification(contentText)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(contentText: String): Notification =
        NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setContentTitle("Importing Raster Layer…")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setSilent(true)
            .build()

    companion object {
        const val NOTIF_ID = 3
        const val NOTIF_CHANNEL_ID = "mbtiles_import"

        const val KEY_URI = "uri"
        const val KEY_BYTES_COPIED = "bytes_copied"
        const val KEY_BYTES_TOTAL = "bytes_total"
        const val KEY_LAYER_NAME = "layer_name"
        const val KEY_ERROR = "error"

        private const val BUFFER_SIZE = 65_536
        private const val PROGRESS_INTERVAL = 4 * 1_048_576L // 4 MB
    }
}
