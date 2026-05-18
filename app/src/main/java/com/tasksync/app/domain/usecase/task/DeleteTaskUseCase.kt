package com.tasksync.app.domain.usecase.task

import com.tasksync.app.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String) {
        require(taskId.isNotBlank()) { "Task ID tidak boleh kosong" }
        taskRepository.deleteTask(taskId)
    }
}