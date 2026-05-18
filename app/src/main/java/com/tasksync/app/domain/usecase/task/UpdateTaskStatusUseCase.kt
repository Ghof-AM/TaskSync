package com.tasksync.app.domain.usecase.task

import com.tasksync.app.domain.model.TaskStatus
import com.tasksync.app.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, status: TaskStatus) {
        require(taskId.isNotBlank()) { "Task ID tidak boleh kosong" }
        taskRepository.updateStatus(taskId, status.value)
    }
}