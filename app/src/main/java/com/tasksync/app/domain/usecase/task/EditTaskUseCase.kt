package com.tasksync.app.domain.usecase.task

import com.tasksync.app.data.remote.FcmSender
import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.domain.repository.UserRepository
import javax.inject.Inject

class EditTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val userRepository: UserRepository,
    private val fcmSender: FcmSender
) {
    suspend operator fun invoke(task: Task) {
        android.util.Log.d("EditTaskUseCase", "EditTaskUseCase called for: ${task.title}")
        require(task.title.isNotBlank()) { "Judul task tidak boleh kosong" }

        taskRepository.updateTask(task)

        try {
            val user = userRepository.getCurrentUser()
            activityLogRepository.addLog(
                ActivityLog(
                    projectId = task.projectId,
                    actorId = user?.id ?: "",
                    actorName = user?.name ?: "Unknown",
                    eventType = LogEvent.TASK_EDITED,
                    message = "${user?.name ?: "Someone"} mengedit task: '${task.title}'"
                )
            )
            // Inject fcmSender dan tambahkan di bagian assign:
            if (task.assignedTo.isNotBlank() &&
                task.assignedToName.isNotBlank() &&
                task.assignedTo != (userRepository.getCurrentUser()?.id ?: "")
            ) {
                fcmSender.sendToUser(
                    targetUserId = task.assignedTo,
                    title = "Task Di-assign ke Kamu!",
                    body = "${user?.name ?: "Someone"} menugaskan '${task.title}' kepadamu",
                    data = mapOf(
                        "taskId" to task.id,
                        "projectId" to task.projectId,
                        "type" to "task_assigned"
                    )
                )
            }
            if (task.assignedTo.isNotBlank() && task.assignedToName.isNotBlank()) {
                activityLogRepository.addLog(
                    ActivityLog(
                        projectId = task.projectId,
                        actorId = user?.id ?: "",
                        actorName = user?.name ?: "Unknown",
                        eventType = LogEvent.TASK_ASSIGNED,
                        message = "Task '${task.title}' di-assign ke ${task.assignedToName}",
                        targetId = task.assignedTo
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("EditTaskUseCase", "Log failed: ${e.message}")
        }
    }
}