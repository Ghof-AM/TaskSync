package com.tasksync.app.domain.model

data class ActivityLog(
    val id: String = "",
    val projectId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val eventType: LogEvent = LogEvent.TASK_CREATED,
    val message: String = "",
    val targetId: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)