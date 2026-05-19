package com.tasksync.app.data.repository

import com.tasksync.app.data.local.dao.CommentDao
import com.tasksync.app.data.mapper.toDomain
import com.tasksync.app.data.mapper.toEntity
import com.tasksync.app.data.remote.FirestoreService
import com.tasksync.app.domain.model.Comment
import com.tasksync.app.domain.repository.CommentRepository
import com.tasksync.app.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val commentDao: CommentDao,
    private val firestoreService: FirestoreService,
    private val networkMonitor: NetworkMonitor
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
}