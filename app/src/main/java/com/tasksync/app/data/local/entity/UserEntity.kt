package com.tasksync.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val fcmToken: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)