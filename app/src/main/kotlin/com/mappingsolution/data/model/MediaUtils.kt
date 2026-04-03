package com.mappingsolution.data.model

import android.media.MediaMetadataRetriever
import java.io.File

object MediaUtils {
    fun createMediaItem(path: String, index: Int): MediaItem {
        return MediaItem(
            id = index.toString(),
            path = path,
            type = getMediaType(path),
            durationMs = getDuration(path)
        )
    }

    fun getDuration(path: String): Long? {
        if (!isVideo(path) && !isAudio(path)) return null
        
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    fun isVideo(path: String): Boolean {
        val lowerPath = path.lowercase()
        return lowerPath.endsWith(".mp4") ||
               lowerPath.endsWith(".3gp") ||
               lowerPath.endsWith(".3gpp") ||
               lowerPath.endsWith(".mkv") ||
               lowerPath.endsWith(".x-matroska") ||
               lowerPath.endsWith(".webm")
    }

    fun isAudio(path: String): Boolean {
        val lowerPath = path.lowercase()
        return lowerPath.endsWith(".mp3") ||
               lowerPath.endsWith(".mpeg") ||
               lowerPath.endsWith(".m4a") ||
               lowerPath.endsWith(".x-m4a") ||
               lowerPath.endsWith(".wav") ||
               lowerPath.endsWith(".x-wav") ||
               lowerPath.endsWith(".aac") ||
               lowerPath.endsWith(".amr") ||
               lowerPath.endsWith(".ogg") ||
               lowerPath.endsWith(".flac")
    }

    fun getMediaType(path: String): MediaType {
        return when {
            isVideo(path) -> MediaType.VIDEO
            isAudio(path) -> MediaType.AUDIO
            else -> MediaType.PHOTO
        }
    }
}
