package com.tasksync.app.domain.repository

import com.tasksync.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(projectId: String): Flow<List<Task>>
    fun getMyTasks(userId: String): Flow<List<Task>>
    suspend fun getTaskById(taskId: String): Task?
    suspend fun createTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: String)
    suspend fun updateStatus(taskId: String, status: String)
    suspend fun syncAllPending()
}