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

    /**
     * Mulai mendengarkan perubahan task dari Firestore secara real-time.
     * Data yang masuk langsung di-upsert ke Room, sehingga getAllTasks() Flow
     * yang sudah dipakai UI akan otomatis ter-trigger tanpa perubahan di ViewModel.
     *
     * Harus dipanggil saat user membuka halaman task sebuah project.
     * Listener berhenti otomatis saat scope yang dipakai dibatalkan.
     */
    fun startListening(projectId: String)
}