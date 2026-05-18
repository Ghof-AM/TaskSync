package com.tasksync.app.domain.usecase.comment

import com.tasksync.app.domain.repository.CommentRepository
import javax.inject.Inject

class DeleteCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(commentId: String) {
        require(commentId.isNotBlank()) { "Comment ID tidak boleh kosong" }
        commentRepository.deleteComment(commentId)
    }
}