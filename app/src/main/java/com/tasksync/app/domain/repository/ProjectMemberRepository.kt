package com.tasksync.app.domain.repository

import com.tasksync.app.domain.model.ProjectMember
import com.tasksync.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface ProjectMemberRepository {
    fun getMembersByProject(projectId: String): Flow<List<ProjectMember>>
    suspend fun getRole(projectId: String, userId: String): UserRole
    suspend fun addMember(projectId: String, userId: String, role: UserRole)
    suspend fun updateRole(projectId: String, userId: String, role: UserRole)
    suspend fun removeMember(projectId: String, userId: String)
}