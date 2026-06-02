package com.tasksync.app.domain.usecase.log

import com.tasksync.app.domain.model.ActivityLog
import com.tasksync.app.domain.repository.ActivityLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActivityLogUseCase @Inject constructor(
    private val logRepository: ActivityLogRepository
) {
    operator fun invoke(projectId: String): Flow<List<ActivityLog>> =
        logRepository.getLogsByProject(projectId)
}