package com.tasksync.app.domain.usecase.task

import com.tasksync.app.domain.repository.TaskRepository
import javax.inject.Inject

class SyncTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke() {
        taskRepository.syncAllPending()
    }
}