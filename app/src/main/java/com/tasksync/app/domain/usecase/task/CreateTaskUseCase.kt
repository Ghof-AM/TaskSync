package com.tasksync.app.domain.usecase.task

import android.util.Log
import com.tasksync.app.data.remote.FcmSender  // ← pastikan import ini ada
import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.domain.repository.UserRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val userRepository: UserRepository,
    private val fcmSender: FcmSender
) {
    suspend operator fun invoke(task: Task) {
        require(task.title.isNotBlank()) { "Judul task tidak boleh kosong" }
        require(task.projectId.isNotBlank()) { "Project ID tidak boleh kosong" }
        require(task.createdBy.isNotBlank()) { "Creator tidak boleh kosong" }

        taskRepository.createTask(task)

        try {
            val creator = userRepository.getCurrentUser()

            activityLogRepository.addLog(
                ActivityLog(
                    projectId = task.projectId,
                    actorId = task.createdBy,
                    actorName = creator?.name ?: "Unknown",
                    eventType = LogEvent.TASK_CREATED,
                    message = "${creator?.name ?: "Someone"} membuat task: ${task.title}"
                )
            )

            if (task.assignedTo.isNotBlank() &&
                task.assignedTo != task.createdBy
            ) {
                activityLogRepository.addLog(
                    ActivityLog(
                        projectId = task.projectId,
                        actorId = task.createdBy,
                        actorName = creator?.name ?: "Unknown",
                        eventType = LogEvent.TASK_ASSIGNED,
                        message = "Task '${task.title}' di-assign ke ${task.assignedToName}",
                        targetId = task.assignedTo
                    )
                )

                // Kirim push notification ke assignee
                fcmSender.sendToUser(
                    targetUserId = task.assignedTo,
                    title = "Task Baru Untukmu!",
                    body = "${creator?.name ?: "Someone"} menugaskan '${task.title}' kepadamu",
                    data = mapOf(
                        "taskId" to task.id,
                        "projectId" to task.projectId,
                        "type" to "task_assigned"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("CreateTaskUseCase", "Error: ${e.message}")
        }
    }
}