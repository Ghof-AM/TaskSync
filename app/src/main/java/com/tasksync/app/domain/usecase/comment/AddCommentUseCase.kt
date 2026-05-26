package com.tasksync.app.domain.usecase.comment

import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.model.Comment
import com.tasksync.app.domain.model.LogEvent
import com.tasksync.app.domain.model.User
import com.tasksync.app.domain.repository.ActivityLogRepository
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.domain.repository.TaskRepository
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
    private val activityLogRepository: ActivityLogRepository,
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, content: String, user: User) {
        require(content.isNotBlank()) { "Komentar tidak boleh kosong" }
        require(content.length <= 500) { "Komentar maksimal 500 karakter" }

        val comment = Comment(
            id = java.util.UUID.randomUUID().toString(), // pastikan fresh UUID
            taskId = taskId,
            userId = user.id,
            userName = user.name,
            content = content.trim()
        )
        commentRepository.addComment(comment)

        // Log komentar ke activity log
        try {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                activityLogRepository.addLog(
                    ActivityLog(
                        projectId = task.projectId,
                        actorId = user.id,
                        actorName = user.name,
                        eventType = LogEvent.TASK_COMMENTED,  // ganti ini
                        message = "${user.name} berkomentar di task '${task.title}': \"${
                            content.trim().take(50)
                        }${if (content.length > 50) "..." else ""}\""
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AddCommentUseCase", "Log failed: ${e.message}")
        }
    }
}