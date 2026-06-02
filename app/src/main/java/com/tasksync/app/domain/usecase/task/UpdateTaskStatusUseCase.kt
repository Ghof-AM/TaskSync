package com.tasksync.app.domain.usecase.task

import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.model.TaskStatus
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.domain.repository.UserRepository
import javax.inject.Inject

class UpdateTaskStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(taskId: String, status: TaskStatus) {
        require(taskId.isNotBlank()) { "Task ID tidak boleh kosong" }

        val task = taskRepository.getTaskById(taskId) ?: return
        taskRepository.updateStatus(taskId, status.value)

        try {
            val user = userRepository.getCurrentUser()
            val statusText = when (status) {
                TaskStatus.TODO -> "Todo"
                TaskStatus.IN_PROGRESS -> "In Progress"
                TaskStatus.DONE -> "Selesai"
            }
            activityLogRepository.addLog(
                ActivityLog(
                    projectId = task.projectId,
                    actorId = user?.id ?: "",
                    actorName = user?.name ?: "Unknown",
                    eventType = LogEvent.TASK_STATUS_CHANGED,
                    message = "${user?.name ?: "Someone"} mengubah status " +
                            "'${task.title}' menjadi $statusText"
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("UpdateStatusUseCase", "Log failed: ${e.message}")
        }
    }
}