package com.tasksync.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: String = "",
    val teamId: String = "",
    val title: String = "",
    val description: String = "",
    val assignedTo: String = "",
    val createdBy: String = "",
    val status: String = "todo",
    val priority: String = "medium",
    val deadline: Long = 0L,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)