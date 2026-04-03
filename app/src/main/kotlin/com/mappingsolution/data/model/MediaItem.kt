package com.mappingsolution.data.model

enum class MediaType {
    PHOTO, VIDEO, AUDIO
}

data class MediaItem(
    val id: String,
    val path: String,
    val type: MediaType,
    val durationMs: Long? = null
)
