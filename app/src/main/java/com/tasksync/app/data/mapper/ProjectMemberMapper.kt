package com.tasksync.app.data.mapper

import com.tasksync.app.data.local.entity.ProjectMemberEntity
import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.UserRole

fun ProjectMemberEntity.toDomain(): ProjectMember = ProjectMember(
    projectId = projectId,
    userId = userId,
    userName = userName,        // tambahkan
    userEmail = userEmail,      // tambahkan
    role = UserRole.fromValue(role),
    joinedAt = joinedAt
)

fun ProjectMember.toEntity(): ProjectMemberEntity = ProjectMemberEntity(
    projectId = projectId,
    userId = userId,
    role = role.value,
    userName = userName,        // tambahkan
    userEmail = userEmail,      // tambahkan
    joinedAt = joinedAt,
    isSynced = false
)