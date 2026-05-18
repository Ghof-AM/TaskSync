package com.tasksync.app.domain.model

data class Task(
    val id: String = "",
    val projectId: String = "",
    val teamId: String = "",
    val title: String = "",
    val description: String = "",
    val assignedTo: String = "",
    val assignedToName: String = "",
    val createdBy: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val priority: Priority = Priority.MEDIUM,
    val deadline: Long = 0L,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)