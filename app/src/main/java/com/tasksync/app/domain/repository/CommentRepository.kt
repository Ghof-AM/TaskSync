package com.tasksync.app.domain.repository

import com.tasksync.app.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun getCommentsByTask(taskId: String): Flow<List<Comment>>
    suspend fun addComment(comment: Comment)
    suspend fun deleteComment(commentId: String)
    suspend fun syncAllPending()
}