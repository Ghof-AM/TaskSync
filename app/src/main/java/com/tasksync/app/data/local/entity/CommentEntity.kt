package com.tasksync.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class CommentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskId: String = "",
    val userId: String = "",
    val userName: String = "",
    val content: String = "",
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)