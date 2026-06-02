package com.tasksync.app.domain.model

import java.util.UUID

data class ActivityLog(
    val id: String = UUID.randomUUID().toString(),  // ← pastikan ada default UUID
    val projectId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val eventType: LogEvent = LogEvent.TASK_CREATED,
    val message: String = "",
    val targetId: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)