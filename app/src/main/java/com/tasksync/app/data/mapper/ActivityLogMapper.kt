package com.tasksync.app.data.mapper

import com.tasksync.app.data.local.entity.ActivityLogEntity
import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent

fun ActivityLogEntity.toDomain(): ActivityLog = ActivityLog(
    id = id,
    projectId = projectId,
    actorId = actorId,
    actorName = actorName,
    eventType = runCatching { LogEvent.valueOf(eventType) }.getOrDefault(LogEvent.TASK_CREATED),
    message = message,
    targetId = targetId,
    isSynced = isSynced,
    createdAt = createdAt
)

fun ActivityLog.toEntity(): ActivityLogEntity = ActivityLogEntity(
    id = id,
    projectId = projectId,
    actorId = actorId,
    actorName = actorName,
    eventType = eventType.name,
    message = message,
    targetId = targetId,
    isSynced = isSynced,
    createdAt = createdAt
)

fun ActivityLog.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "projectId" to projectId,
    "actorId" to actorId,
    "actorName" to actorName,
    "eventType" to eventType.name,
    "message" to message,
    "targetId" to targetId,
    "createdAt" to createdAt
)