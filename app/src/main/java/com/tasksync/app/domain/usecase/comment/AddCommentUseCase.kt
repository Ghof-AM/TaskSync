package com.tasksync.app.domain.usecase.comment

import com.tasksync.app.domain.model.Comment
import com.tasksync.app.domain.model.User
import com.tasksync.app.domain.repository.CommentRepository
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(taskId: String, content: String, user: User) {
        require(content.isNotBlank()) { "Komentar tidak boleh kosong" }
        require(content.length <= 500) { "Komentar maksimal 500 karakter" }
        val comment = Comment(
            taskId = taskId,
            userId = user.id,
            userName = user.name,
            content = content.trim()
        )
        commentRepository.addComment(comment)
    }
}