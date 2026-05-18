package com.tasksync.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "activity_log")
data class ActivityLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val eventType: String = "",
    val message: String = "",
    val targetId: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)