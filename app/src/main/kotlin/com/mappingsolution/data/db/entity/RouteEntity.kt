package com.mappingsolution.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val color: String = "#FFFF5722",
    val name: String,
    val description: String? = null,
    val isVisible: Boolean = true,
    /** false until the user explicitly taps Stop; used to detect incomplete recordings on relaunch */
    val didUserTapStop: Boolean = false,
    val startedAt: Long,
    val stoppedAt: Long? = null,
    val checkpointAt: Long,
    val distanceMeters: Double = 0.0,
    val durationSec: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
