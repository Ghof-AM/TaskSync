package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.TaskDao
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.mapper.toEntity
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.Priority
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.model.TaskStatus
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val firestoreService: FirestoreService,
    private val networkMonitor: NetworkMonitor
) : TaskRepository {

    // Job untuk listener aktif. Disimpan agar bisa dibatalkan jika perlu
    // (misal: user pindah project atau logout).
    private var listenerJob: Job? = null

    override fun getAllTasks(projectId: String): Flow<List<Task>> =
        taskDao.getAllTasks(projectId).map { list -> list.map { it.toDomain() } }

    override fun getMyTasks(userId: String): Flow<List<Task>> =
        taskDao.getMyTasks(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getTaskById(taskId: String): Task? =
        taskDao.getTaskById(taskId)?.toDomain()

    override fun getTaskFlow(taskId: String): Flow<Task?> =
        taskDao.getTaskByIdFlow(taskId).map { entity -> entity?.toDomain() }
    /**
     * Mulai mendengarkan Firestore secara real-time untuk projectId tertentu.
     *
     * Cara kerja:
     * 1. Firestore mengirim snapshot setiap ada perubahan (create/update/delete)
     *    dari user manapun.
     * 2. Setiap task dari snapshot di-upsert ke Room (insertTask pakai REPLACE).
     * 3. Karena TaskDao.getAllTasks() adalah Flow dari Room, UI langsung
     *    ter-update otomatis tanpa perlu kode tambahan di ViewModel.
     *
     * Listener lama dibatalkan dulu jika ada, sehingga aman dipanggil
     * berkali-kali (misal: user navigasi kembali ke halaman yang sama).
     */
    override fun startListening(projectId: String) {
        listenerJob?.cancel()
        listenerJob = CoroutineScope(Dispatchers.IO).launch {
            firestoreService.listenToTasksByProject(projectId)
                .collect { taskMaps ->
                    taskMaps.forEach { map ->
                        try {
                            val task = map.toTask()
                            // Hanya upsert jika data valid (minimal punya id dan projectId)
                            if (task.id.isNotBlank() && task.projectId.isNotBlank()) {
                                // Tandai isSynced = true karena data ini datang dari Firestore
                                taskDao.insertTask(task.toEntity().copy(isSynced = true))
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("TaskRepo", "Gagal parse task dari Firestore: ${e.message}")
                        }
                    }
                }
        }
    }

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

// ─── Helper: parse Map dari Firestore ke domain Task ─────────────────────────
// Diletakkan di sini agar tidak mencemari TaskMapper yang sudah ada.
private fun Map<String, Any?>.toTask(): Task = Task(
    id           = this["id"] as? String ?: "",
    projectId    = this["projectId"] as? String ?: "",
    teamId       = this["teamId"] as? String ?: "",
    title        = this["title"] as? String ?: "",
    description  = this["description"] as? String ?: "",
    assignedTo   = this["assignedTo"] as? String ?: "",
    assignedToName = this["assignedToName"] as? String ?: "",
    createdBy    = this["createdBy"] as? String ?: "",
    status       = TaskStatus.fromValue(this["status"] as? String ?: "todo"),
    priority     = Priority.fromValue(this["priority"] as? String ?: "medium"),
    deadline     = (this["deadline"] as? Long) ?: 0L,
    isSynced     = true,
    isDeleted    = (this["isDeleted"] as? Boolean) ?: false,
    updatedAt    = (this["updatedAt"] as? Long) ?: System.currentTimeMillis()
)