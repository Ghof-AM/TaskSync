package com.tasksync.app.data.mapper

import com.tasksync.app.data.local.entity.ProjectEntity
import com.tasksync.app.domain.model.Project

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    description = description,
    createdBy = createdBy,
    createdAt = createdAt
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    description = description,
    createdBy = createdBy,
    isSynced = false,
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis()
)

fun Project.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "description" to description,
    "createdBy" to createdBy,
    "createdAt" to createdAt
)