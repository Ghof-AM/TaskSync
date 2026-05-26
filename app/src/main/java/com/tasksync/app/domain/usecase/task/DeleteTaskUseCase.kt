package com.tasksync.app.domain.usecase.task

import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.domain.repository.UserRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(taskId: String) {
        require(taskId.isNotBlank()) { "Task ID tidak boleh kosong" }

        // Ambil task dulu sebelum dihapus untuk dapat judul & projectId
        val task = taskRepository.getTaskById(taskId)

        taskRepository.deleteTask(taskId)

        // Log penghapusan task
        if (task != null) {
            try {
                val user = userRepository.getCurrentUser()
                activityLogRepository.addLog(
                    ActivityLog(
                        projectId = task.projectId,
                        actorId = user?.id ?: "",
                        actorName = user?.name ?: "Unknown",
                        eventType = LogEvent.TASK_DELETED,
                        message = "${user?.name ?: "Someone"} menghapus task: '${task.title}'"
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("DeleteTaskUseCase", "Log failed: ${e.message}")
            }
        }
    }
}