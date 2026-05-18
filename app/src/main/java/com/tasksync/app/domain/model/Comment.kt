package com.tasksync.app.domain.model

data class Comment(
    val id: String = "",
    val taskId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)