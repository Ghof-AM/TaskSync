package com.tasksync.app.data.mapper

import com.tasksync.app.data.local.entity.CommentEntity
import com.tasksync.app.domain.model.Comment

fun CommentEntity.toDomain(): Comment = Comment(
    id = id,
    taskId = taskId,
    userId = userId,
    userName = userName,
    content = content,
    isSynced = isSynced,
    createdAt = createdAt
)

fun Comment.toEntity(): CommentEntity = CommentEntity(
    id = id,
    taskId = taskId,
    userId = userId,
    userName = userName,
    content = content,
    isSynced = isSynced,
    createdAt = createdAt
)

fun Comment.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "taskId" to taskId,
    "userId" to userId,
    "userName" to userName,
    "content" to content,
    "createdAt" to createdAt
)