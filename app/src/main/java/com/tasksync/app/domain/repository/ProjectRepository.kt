package com.tasksync.app.domain.repository

import com.tasksync.app.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjectsByUser(userId: String): Flow<List<Project>>
    suspend fun getProjectById(projectId: String): Project?
    suspend fun createProject(project: Project, creatorId: String)
    suspend fun deleteProject(projectId: String)
    suspend fun syncAllPending()
}