package com.tasksync.app.data.mapper

import com.tasksync.app.data.local.entity.UserEntity
import com.tasksync.app.domain.model.User
import com.tasksync.app.domain.model.UserRole

fun UserEntity.toDomain(): User = User(
    id = id,
    name = name,
    email = email,
    photoUrl = photoUrl,
    fcmToken = fcmToken
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    name = name,
    email = email,
    photoUrl = photoUrl,
    fcmToken = fcmToken,
    updatedAt = System.currentTimeMillis()
)

fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "email" to email,
    "photoUrl" to photoUrl,
    "fcmToken" to fcmToken
)

fun Map<String, Any?>.toUser(): User = User(
    id = this["id"] as? String ?: "",
    name = this["name"] as? String ?: "",
    email = this["email"] as? String ?: "",
    photoUrl = this["photoUrl"] as? String ?: "",
    fcmToken = this["fcmToken"] as? String ?: "",
    role = UserRole.fromValue(this["role"] as? String ?: "member")
)