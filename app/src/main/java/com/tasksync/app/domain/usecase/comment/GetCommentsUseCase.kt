package com.tasksync.app.domain.usecase.comment

import com.tasksync.app.domain.model.Comment
import com.tasksync.app.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    operator fun invoke(taskId: String): Flow<List<Comment>> =
        commentRepository.getCommentsByTask(taskId)
}