package com.tasksync.app.domain.repository

import com.tasksync.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getCurrentUser(): User?
    suspend fun getUserById(userId: String): User?
    fun getUsersByIds(userIds: List<String>): Flow<List<User>>
    suspend fun saveUser(user: User)
    suspend fun updateFcmToken(token: String)
    suspend fun updateProfile(name: String, photoUrl: String)
}