package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.TaskDao
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.mapper.toEntity
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val firestoreService: FirestoreService,
    private val networkMonitor: NetworkMonitor
) : TaskRepository {

    override fun getAllTasks(projectId: String): Flow<List<Task>> =
        taskDao.getAllTasks(projectId).map { list -> list.map { it.toDomain() } }

    override fun getMyTasks(userId: String): Flow<List<Task>> =
        taskDao.getMyTasks(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getTaskById(taskId: String): Task? =
        taskDao.getTaskById(taskId)?.toDomain()

    override suspend fun createTask(task: Task) {
        val entity = task.toEntity().copy(isSynced = false)
        taskDao.insertTask(entity)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadTask(task)
                taskDao.markAsSynced(task.id)
            } catch (e: Exception) {
                // WorkManager akan retry
            }
        }
    }

    override suspend fun updateTask(task: Task) {
        val entity = task.toEntity().copy(
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(entity)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadTask(task)
                taskDao.markAsSynced(task.id)
            } catch (e: Exception) {
                // WorkManager akan retry
            }
        }
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.softDelete(taskId)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.deleteTaskRemote(taskId)
            } catch (e: Exception) {
                // WorkManager akan retry
            }
        }
    }

    override suspend fun updateStatus(taskId: String, status: String) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updated = task.copy(
            status = status,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        taskDao.updateTask(updated)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadTask(updated.toDomain())
                taskDao.markAsSynced(taskId)
            } catch (e: Exception) {
                // WorkManager akan retry
            }
        }
    }

    override suspend fun syncAllPending() {
        taskDao.getUnsyncedTasks().forEach { entity ->
            try {
                firestoreService.uploadTask(entity.toDomain())
                taskDao.markAsSynced(entity.id)
            } catch (e: Exception) { /* skip, retry next cycle */ }
        }
        taskDao.getDeletedUnsyncedTasks().forEach { entity ->
            try {
                firestoreService.deleteTaskRemote(entity.id)
                taskDao.markAsSynced(entity.id)
            } catch (e: Exception) { /* skip */ }
        }
    }
}