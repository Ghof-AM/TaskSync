package com.tasksync.app.domain.model

data class ProjectMember(
    val projectId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoUrl: String = "",
    val role: UserRole = UserRole.MEMBER,
    val joinedAt: Long = System.currentTimeMillis()
)