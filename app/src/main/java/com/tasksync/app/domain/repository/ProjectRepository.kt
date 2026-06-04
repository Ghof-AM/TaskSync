package com.tasksync.app.domain.repository

import com.tasksync.app.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjectsByUser(userId: String): Flow<List<Project>>
    suspend fun getProjectById(projectId: String): Project?
    suspend fun createProject(project: Project, creatorId: String)
    suspend fun deleteProject(projectId: String)
    suspend fun syncAllPending()
    suspend fun fetchRemoteProjectsAndNotify(userId: String)

    /**
     * Mulai mendengarkan perubahan project dari Firestore secara real-time.
     * Ketika user diundang ke project baru oleh user lain, project tersebut
     * akan otomatis muncul di daftar project tanpa perlu menunggu SyncWorker.
     *
     * Harus dipanggil saat user membuka halaman daftar project.
     * Listener berhenti otomatis saat scope yang dipakai dibatalkan.
     */
    fun startListening(userId: String)
}