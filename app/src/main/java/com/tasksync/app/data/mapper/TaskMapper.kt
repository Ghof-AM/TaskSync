package com.tasksync.app.data.mapper

import com.tasksync.app.data.local.entity.TaskEntity
import com.tasksync.app.domain.model.Priority
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.TaskStatus

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    projectId = projectId,
    teamId = teamId,
    title = title,
    description = description,
    assignedTo = assignedTo,
    assignedToName = assignedToName,  // ← tambahkan
    createdBy = createdBy,
    status = TaskStatus.fromValue(status),
    priority = Priority.fromValue(priority),
    deadline = deadline,
    isSynced = isSynced,
    isDeleted = isDeleted,
    updatedAt = updatedAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    projectId = projectId,
    teamId = teamId,
    title = title,
    description = description,
    assignedTo = assignedTo,
    assignedToName = assignedToName,  // ← tambahkan
    createdBy = createdBy,
    status = status.value,
    priority = priority.value,
    deadline = deadline,
    isSynced = isSynced,
    isDeleted = isDeleted,
    updatedAt = updatedAt
)

fun Task.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "projectId" to projectId,
    "teamId" to teamId,
    "title" to title,
    "description" to description,
    "assignedTo" to assignedTo,
    "assignedToName" to assignedToName,  // ← tambahkan
    "createdBy" to createdBy,
    "status" to status.value,
    "priority" to priority.value,
    "deadline" to deadline,
    "isDeleted" to isDeleted,
    "updatedAt" to updatedAt
)