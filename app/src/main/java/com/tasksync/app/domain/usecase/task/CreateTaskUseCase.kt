package com.tasksync.app.domain.usecase.task

import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.repository.TaskRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        require(task.title.isNotBlank()) { "Judul task tidak boleh kosong" }
        require(task.projectId.isNotBlank()) { "Project ID tidak boleh kosong" }
        require(task.createdBy.isNotBlank()) { "Creator tidak boleh kosong" }
        taskRepository.createTask(task)
    }
}