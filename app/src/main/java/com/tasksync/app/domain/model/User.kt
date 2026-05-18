package com.tasksync.app.domain.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val fcmToken: String = "",
    val role: UserRole = UserRole.MEMBER
)