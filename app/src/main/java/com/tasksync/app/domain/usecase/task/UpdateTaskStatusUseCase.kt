package com.tasksync.app.domain.usecase.task

import com.tasksync.app.data.remote.FcmSender
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
    private val userRepository: UserRepository,
    private val fcmSender: FcmSender
) {
    suspend operator fun invoke(taskId: String, status: TaskStatus) {
        require(taskId.isNotBlank()) { "Task ID tidak boleh kosong" }

        val task = taskRepository.getTaskById(taskId) ?: return
        taskRepository.updateStatus(taskId, status.value)

        try {
            val actor = userRepository.getCurrentUser()
            val statusText = when (status) {
                TaskStatus.TODO -> "Todo"
                TaskStatus.IN_PROGRESS -> "In Progress"
                TaskStatus.DONE -> "Selesai"
            }

            activityLogRepository.addLog(
                ActivityLog(
                    projectId = task.projectId,
                    actorId = actor?.id ?: "",
                    actorName = actor?.name ?: "Unknown",
                    eventType = LogEvent.TASK_STATUS_CHANGED,
                    message = "${actor?.name ?: "Someone"} mengubah status " +
                            "'${task.title}' menjadi $statusText"
                )
            )

            // Kirim notif ke assignee jika yang mengubah bukan assignee itu sendiri
            if (task.assignedTo.isNotBlank() && task.assignedTo != actor?.id) {
                fcmSender.sendToUser(
                    targetUserId = task.assignedTo,
                    title = "Status Task Berubah",
                    body = "${actor?.name ?: "Someone"} mengubah '${task.title}' menjadi $statusText",
                    data = mapOf(
                        "type" to "task_status_changed",
                        "taskId" to task.id,
                        "projectId" to task.projectId
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateStatusUseCase", "Notif/log failed: ${e.message}")
        }
    }
}