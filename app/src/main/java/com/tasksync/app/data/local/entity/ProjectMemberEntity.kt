package com.tasksync.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "project_members",
    primaryKeys = ["projectId", "userId"],
    indices = [Index(value = ["projectId"]), Index(value = ["userId"])]
)
data class ProjectMemberEntity(
    val projectId: String = "",
    val userId: String = "",
    val role: String = "member",
    val userName: String = "",      // tambahkan
    val userEmail: String = "",     // tambahkan
    val joinedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)