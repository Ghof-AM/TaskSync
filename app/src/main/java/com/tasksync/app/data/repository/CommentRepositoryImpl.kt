package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.CommentDao
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.mapper.toEntity
import com.tasksync.app.data.remote.FcmSender
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.Comment
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.domain.repository.TaskRepository
import com.tasksync.app.domain.repository.UserRepository
import com.tasksync.app.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val commentDao: CommentDao,
    private val firestoreService: FirestoreService,
    private val networkMonitor: NetworkMonitor,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val fcmSender: FcmSender
) : CommentRepository {

    override fun getCommentsByTask(taskId: String): Flow<List<Comment>> =
        commentDao.getCommentsByTask(taskId).map { list -> list.map { it.toDomain() } }

    override suspend fun addComment(comment: Comment) {
        val entity = comment.toEntity().copy(isSynced = false)
        commentDao.insertComment(entity)

        if (networkMonitor.isOnline()) {
            try {
                firestoreService.uploadComment(comment)
                commentDao.markAsSynced(comment.id)
            } catch (e: Exception) { /* WorkManager retry */ }
        }

        // Kirim FCM ke assignee task jika yang komentar bukan assignee itu sendiri
        try {
            val task = taskRepository.getTaskById(comment.taskId)
            if (task != null &&
                task.assignedTo.isNotBlank() &&
                task.assignedTo != comment.userId
            ) {
                fcmSender.sendToUser(
                    targetUserId = task.assignedTo,
                    title = "Komentar Baru di Task",
                    body = "${comment.userName}: ${comment.content.take(80)}",
                    data = mapOf(
                        "type" to "new_comment",
                        "taskId" to comment.taskId,
                        "projectId" to task.projectId
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("CommentRepo", "FCM comment notif failed: ${e.message}")
        }
    }

    override suspend fun deleteComment(commentId: String) {
        commentDao.deleteComment(commentId)
        if (networkMonitor.isOnline()) {
            try {
                firestoreService.deleteCommentRemote(commentId)
            } catch (e: Exception) { /* skip */ }
        }
    }

    override suspend fun syncAllPending() {
        commentDao.getUnsyncedComments().forEach { entity ->
            try {
                firestoreService.uploadComment(entity.toDomain())
                commentDao.markAsSynced(entity.id)
            } catch (e: Exception) { /* skip */ }
        }
    }

    override suspend fun pullFromFirestore(taskId: String) {
        if (!networkMonitor.isOnline()) return
        try {
            val remoteComments = firestoreService.getCommentsByTask(taskId)
            remoteComments.forEach { data ->
                val id = data["id"] as? String ?: return@forEach
                val existing = commentDao.getCommentById(id)
                if (existing == null) {
                    val comment = Comment(
                        id = id,
                        taskId = data["taskId"] as? String ?: "",
                        userId = data["userId"] as? String ?: "",
                        userName = data["userName"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        isSynced = true,
                        createdAt = (data["createdAt"] as? Long) ?: 0L
                    )
                    commentDao.insertComment(comment.toEntity().copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CommentRepo", "Pull error: ${e.message}")
        }
    }
}