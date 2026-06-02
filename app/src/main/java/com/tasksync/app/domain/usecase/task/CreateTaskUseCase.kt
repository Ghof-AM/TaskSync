package com.tasksync.app.domain.usecase.task

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
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(task: Task) {
        require(task.title.isNotBlank()) { "Judul task tidak boleh kosong" }
        require(task.projectId.isNotBlank()) { "Project ID tidak boleh kosong" }
        require(task.createdBy.isNotBlank()) { "Creator tidak boleh kosong" }

        // Simpan task
        taskRepository.createTask(task)

        // Catat ke activity log
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

            // Log assign jika ada assignee
            if (task.assignedTo.isNotBlank() && task.assignedToName.isNotBlank()) {
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
            }
        } catch (e: Exception) {
            android.util.Log.e("CreateTaskUseCase", "Log failed: ${e.message}")
        }
    }
}