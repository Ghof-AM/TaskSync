package com.tasksync.app.domain.usecase.task

import com.tasksync.app.domain.model.Task
import com.tasksync.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    operator fun invoke(projectId: String): Flow<List<Task>> =
        taskRepository.getAllTasks(projectId)
}